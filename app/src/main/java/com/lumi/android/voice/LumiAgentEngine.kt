package com.lumi.android.voice

class LumiAgentEngine(private val planner: LumiTaskPlanner) {
    data class AgentPlan(
        val original: String,
        val tasks: List<LumiTask>,
        val requiresConfirmation: Boolean
    )

    fun plan(request: String): AgentPlan? {
        val plan = planner.plan(request) ?: return null
        return AgentPlan(plan.originalRequest, plan.tasks, plan.requiresConfirmation)
    }
}
