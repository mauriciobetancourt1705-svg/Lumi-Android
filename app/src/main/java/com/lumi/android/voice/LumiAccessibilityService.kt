package com.lumi.android.voice

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Control bridge for Lumi.
 *
 * This service is intentionally small: the LLM never receives direct Android
 * privileges. It asks LumiAndroidActionExecutor for a typed action and this
 * service performs only the Android accessibility operation requested.
 */
class LumiAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 80
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    fun clickText(text: String): Boolean =
        findNodeByText(text)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    fun clickDescription(description: String): Boolean =
        findNodeByDescription(description)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    fun setText(text: String): Boolean {
        val node = findFocusedEditable() ?: findFirstEditable(rootInActiveWindow) ?: return false
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scrollForward(): Boolean =
        findScrollable(rootInActiveWindow)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true

    fun scrollBackward(): Boolean =
        findScrollable(rootInActiveWindow)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true

    fun describeScreen(): String {
        val root = rootInActiveWindow ?: return ""
        val out = StringBuilder()
        appendNodeSummary(root, out, 0)
        return out.toString().trim()
    }

    private fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val exact = root.findAccessibilityNodeInfosByText(text)
        return exact.firstOrNull { it.isVisibleToUser }
            ?: exact.firstOrNull()
    }

    private fun findNodeByDescription(description: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findNode(root) {
            it.isVisibleToUser &&
                it.contentDescription?.toString()?.contains(description, ignoreCase = true) == true
        }
    }

    private fun findFocusedEditable(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findNode(root) { it.isEditable && it.isFocused && it.isVisibleToUser }
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? =
        node?.let {
            if (it.isEditable && it.isVisibleToUser) return it
            for (i in 0 until it.childCount) {
                findFirstEditable(it.getChild(i))?.let { found -> return found }
            }
            null
        }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? =
        findNode(node) { it.isScrollable && it.isVisibleToUser }

    private fun findNode(
        node: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        for (i in 0 until node.childCount) {
            findNode(node.getChild(i), predicate)?.let { return it }
        }
        return null
    }

    private fun appendNodeSummary(
        node: AccessibilityNodeInfo,
        out: StringBuilder,
        depth: Int
    ) {
        if (depth > 8) return
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        if ((text.isNotBlank() || desc.isNotBlank()) && node.isVisibleToUser) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            out.append(" ".repeat(depth * 2))
                .append(if (text.isNotBlank()) text else desc)
                .append(" [").append(bounds.left).append(",")
                .append(bounds.top).append("..")
                .append(bounds.right).append(",")
                .append(bounds.bottom).append("]")
                .append(if (node.isClickable) " clickable" else "")
                .append(if (node.isEditable) " editable" else "")
                .append('\n')
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { appendNodeSummary(it, out, depth + 1) }
        }
    }

    companion object {
        @Volatile
        private var instance: LumiAccessibilityService? = null

        fun current(): LumiAccessibilityService? = instance

        fun isEnabled(): Boolean = instance != null
    }
}
