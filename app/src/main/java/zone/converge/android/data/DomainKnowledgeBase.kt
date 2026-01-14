// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.data

/**
 * Domain Knowledge Base for Converge.
 *
 * Contains the canonical domain model aligned with iOS:
 * - 5 Core Packs: Money, Customers, Delivery, People, Trust
 * - 15 Core JTBDs (Jobs To Be Done)
 * - 5 Blueprints: Lead-to-Cash, Invoice-to-Cash, Promise-to-Delivery, etc.
 * - 5 Flows: Deal-Closed, Work-Completed, Payment-Received, etc.
 *
 * ID conventions (matching iOS):
 * - Packs: "pack-{name}" e.g., "pack-money"
 * - JTBDs: "jtbd-{verb}-{object}" e.g., "jtbd-issue-invoice"
 * - Blueprints: "blueprint-{name}" e.g., "blueprint-lead-to-cash"
 * - Flows: "flow-{name}" e.g., "flow-deal-closed"
 */
object DomainKnowledgeBase {

    // ================================
    // PACKS
    // ================================

    val packs: List<DomainPack> = listOf(
        DomainPack(
            id = "pack-money",
            name = "Money",
            description = "Financial operations: invoicing, payments, reconciliation, period close",
            icon = "dollarsign.circle",
            relatedPacks = listOf("pack-customers", "pack-delivery", "pack-trust"),
        ),
        DomainPack(
            id = "pack-customers",
            name = "Customers",
            description = "Revenue generation: leads, opportunities, proposals, deals",
            icon = "person.2",
            relatedPacks = listOf("pack-money", "pack-delivery"),
        ),
        DomainPack(
            id = "pack-delivery",
            name = "Delivery",
            description = "Value delivery: promises, work items, blockers, acceptance",
            icon = "shippingbox",
            relatedPacks = listOf("pack-customers", "pack-money", "pack-people"),
        ),
        DomainPack(
            id = "pack-people",
            name = "People",
            description = "Workforce: hiring, onboarding, access, payroll",
            icon = "person.3",
            relatedPacks = listOf("pack-delivery", "pack-money", "pack-trust"),
        ),
        DomainPack(
            id = "pack-trust",
            name = "Trust",
            description = "Governance: audit trails, compliance, risk management",
            icon = "shield.checkered",
            relatedPacks = listOf("pack-money", "pack-people", "pack-customers"),
        ),
    )

    // ================================
    // JOBS TO BE DONE (JTBDs)
    // ================================

    val jtbds: Map<String, JTBD> = mapOf(
        // === Money Pack JTBDs ===
        "jtbd-issue-invoice" to JTBD(
            id = "jtbd-issue-invoice",
            packId = "pack-money",
            name = "Issue Invoice",
            verb = "Issue",
            obj = "invoice",
            outcome = "Customer receives accurate invoice with all deliverables documented",
            prerequisites = listOf("jtbd-confirm-acceptance"),
            nextSteps = listOf("jtbd-collect-payment"),
            requiredArtifacts = listOf(ArtifactType.ACCEPTANCE, ArtifactType.DEAL),
            producedArtifacts = listOf(ArtifactType.INVOICE),
        ),
        "jtbd-collect-payment" to JTBD(
            id = "jtbd-collect-payment",
            packId = "pack-money",
            name = "Collect Payment",
            verb = "Collect",
            obj = "payment",
            outcome = "Payment received and recorded against invoice",
            prerequisites = listOf("jtbd-issue-invoice"),
            nextSteps = listOf("jtbd-reconcile-books"),
            requiredArtifacts = listOf(ArtifactType.INVOICE),
            producedArtifacts = listOf(ArtifactType.PAYMENT),
        ),
        "jtbd-reconcile-books" to JTBD(
            id = "jtbd-reconcile-books",
            packId = "pack-money",
            name = "Reconcile Books",
            verb = "Reconcile",
            obj = "accounts",
            outcome = "All transactions matched and verified",
            prerequisites = listOf("jtbd-collect-payment"),
            nextSteps = listOf("jtbd-close-period"),
            requiredArtifacts = listOf(ArtifactType.PAYMENT),
            producedArtifacts = listOf(ArtifactType.RECONCILIATION),
        ),
        "jtbd-close-period" to JTBD(
            id = "jtbd-close-period",
            packId = "pack-money",
            name = "Close Period",
            verb = "Close",
            obj = "period",
            outcome = "Financial period closed with accurate statements",
            prerequisites = listOf("jtbd-reconcile-books"),
            nextSteps = emptyList(),
            requiredArtifacts = listOf(ArtifactType.RECONCILIATION),
            producedArtifacts = listOf(ArtifactType.PERIOD_CLOSE, ArtifactType.AUDIT_ENTRY),
        ),

        // === Customers Pack JTBDs ===
        "jtbd-capture-lead" to JTBD(
            id = "jtbd-capture-lead",
            packId = "pack-customers",
            name = "Capture Lead",
            verb = "Capture",
            obj = "lead",
            outcome = "Potential customer identified and qualified",
            prerequisites = emptyList(),
            nextSteps = listOf("jtbd-develop-opportunity"),
            requiredArtifacts = listOf(ArtifactType.SIGNAL),
            producedArtifacts = listOf(ArtifactType.LEAD),
        ),
        "jtbd-develop-opportunity" to JTBD(
            id = "jtbd-develop-opportunity",
            packId = "pack-customers",
            name = "Develop Opportunity",
            verb = "Develop",
            obj = "opportunity",
            outcome = "Lead converted to qualified opportunity with clear needs",
            prerequisites = listOf("jtbd-capture-lead"),
            nextSteps = listOf("jtbd-create-proposal"),
            requiredArtifacts = listOf(ArtifactType.LEAD),
            producedArtifacts = listOf(ArtifactType.OPPORTUNITY),
        ),
        "jtbd-create-proposal" to JTBD(
            id = "jtbd-create-proposal",
            packId = "pack-customers",
            name = "Create Proposal",
            verb = "Create",
            obj = "proposal",
            outcome = "Compelling proposal that addresses customer needs",
            prerequisites = listOf("jtbd-develop-opportunity"),
            nextSteps = listOf("jtbd-close-deal"),
            requiredArtifacts = listOf(ArtifactType.OPPORTUNITY, ArtifactType.STRATEGY),
            producedArtifacts = listOf(ArtifactType.PROPOSAL),
        ),
        "jtbd-close-deal" to JTBD(
            id = "jtbd-close-deal",
            packId = "pack-customers",
            name = "Close Deal",
            verb = "Close",
            obj = "deal",
            outcome = "Customer commits with signed contract",
            prerequisites = listOf("jtbd-create-proposal"),
            nextSteps = listOf("jtbd-make-promise"),
            requiredArtifacts = listOf(ArtifactType.PROPOSAL),
            producedArtifacts = listOf(ArtifactType.DEAL, ArtifactType.CONTRACT),
        ),

        // === Delivery Pack JTBDs ===
        "jtbd-make-promise" to JTBD(
            id = "jtbd-make-promise",
            packId = "pack-delivery",
            name = "Make Promise",
            verb = "Make",
            obj = "promise",
            outcome = "Clear commitment on deliverables, timeline, and quality",
            prerequisites = listOf("jtbd-close-deal"),
            nextSteps = listOf("jtbd-execute-work"),
            requiredArtifacts = listOf(ArtifactType.DEAL, ArtifactType.CONTRACT),
            producedArtifacts = listOf(ArtifactType.PROMISE),
        ),
        "jtbd-execute-work" to JTBD(
            id = "jtbd-execute-work",
            packId = "pack-delivery",
            name = "Execute Work",
            verb = "Execute",
            obj = "work",
            outcome = "Work completed according to promise specifications",
            prerequisites = listOf("jtbd-make-promise"),
            nextSteps = listOf("jtbd-confirm-acceptance"),
            requiredArtifacts = listOf(ArtifactType.PROMISE),
            producedArtifacts = listOf(ArtifactType.WORK_ITEM),
        ),
        "jtbd-resolve-blocker" to JTBD(
            id = "jtbd-resolve-blocker",
            packId = "pack-delivery",
            name = "Resolve Blocker",
            verb = "Resolve",
            obj = "blocker",
            outcome = "Impediment removed and work can proceed",
            prerequisites = emptyList(),
            nextSteps = listOf("jtbd-execute-work"),
            requiredArtifacts = listOf(ArtifactType.BLOCKER),
            producedArtifacts = listOf(ArtifactType.DECISION),
        ),
        "jtbd-confirm-acceptance" to JTBD(
            id = "jtbd-confirm-acceptance",
            packId = "pack-delivery",
            name = "Confirm Acceptance",
            verb = "Confirm",
            obj = "acceptance",
            outcome = "Customer confirms work meets requirements",
            prerequisites = listOf("jtbd-execute-work"),
            nextSteps = listOf("jtbd-issue-invoice"),
            requiredArtifacts = listOf(ArtifactType.WORK_ITEM),
            producedArtifacts = listOf(ArtifactType.ACCEPTANCE),
        ),

        // === People Pack JTBDs ===
        "jtbd-onboard-person" to JTBD(
            id = "jtbd-onboard-person",
            packId = "pack-people",
            name = "Onboard Person",
            verb = "Onboard",
            obj = "person",
            outcome = "New team member is productive with proper access and training",
            prerequisites = emptyList(),
            nextSteps = listOf("jtbd-grant-access"),
            requiredArtifacts = listOf(ArtifactType.CONTRACT),
            producedArtifacts = listOf(ArtifactType.EMPLOYEE, ArtifactType.CONTRACTOR),
        ),
        "jtbd-grant-access" to JTBD(
            id = "jtbd-grant-access",
            packId = "pack-people",
            name = "Grant Access",
            verb = "Grant",
            obj = "access",
            outcome = "Person has appropriate system access for their role",
            prerequisites = listOf("jtbd-onboard-person"),
            nextSteps = listOf("jtbd-execute-work"),
            requiredArtifacts = listOf(ArtifactType.EMPLOYEE),
            producedArtifacts = listOf(ArtifactType.ACCESS_GRANT, ArtifactType.AUDIT_ENTRY),
        ),
        "jtbd-run-payroll" to JTBD(
            id = "jtbd-run-payroll",
            packId = "pack-people",
            name = "Run Payroll",
            verb = "Run",
            obj = "payroll",
            outcome = "All team members paid accurately and on time",
            prerequisites = emptyList(),
            nextSteps = listOf("jtbd-reconcile-books"),
            requiredArtifacts = listOf(ArtifactType.EMPLOYEE, ArtifactType.CONTRACTOR),
            producedArtifacts = listOf(ArtifactType.PAYROLL_RUN, ArtifactType.PAYMENT),
        ),

        // === Trust Pack JTBDs ===
        "jtbd-record-compliance" to JTBD(
            id = "jtbd-record-compliance",
            packId = "pack-trust",
            name = "Record Compliance",
            verb = "Record",
            obj = "compliance",
            outcome = "Compliance evidence captured and verified",
            prerequisites = emptyList(),
            nextSteps = emptyList(),
            requiredArtifacts = listOf(ArtifactType.AUDIT_ENTRY),
            producedArtifacts = listOf(ArtifactType.COMPLIANCE_RECORD),
        ),
    )

    // ================================
    // BLUEPRINTS (Multi-Pack Workflows)
    // ================================

    val blueprints: List<Blueprint> = listOf(
        Blueprint(
            id = "blueprint-lead-to-cash",
            name = "Lead to Cash",
            description = "Complete revenue cycle from lead capture through payment collection",
            packIds = listOf("pack-customers", "pack-delivery", "pack-money"),
            jtbdSequence = listOf(
                "jtbd-capture-lead",
                "jtbd-develop-opportunity",
                "jtbd-create-proposal",
                "jtbd-close-deal",
                "jtbd-make-promise",
                "jtbd-execute-work",
                "jtbd-confirm-acceptance",
                "jtbd-issue-invoice",
                "jtbd-collect-payment",
            ),
            icon = "arrow.triangle.2.circlepath",
        ),
        Blueprint(
            id = "blueprint-invoice-to-cash",
            name = "Invoice to Cash",
            description = "Billing through payment collection and reconciliation",
            packIds = listOf("pack-delivery", "pack-money"),
            jtbdSequence = listOf(
                "jtbd-confirm-acceptance",
                "jtbd-issue-invoice",
                "jtbd-collect-payment",
                "jtbd-reconcile-books",
            ),
            icon = "banknote",
        ),
        Blueprint(
            id = "blueprint-promise-to-delivery",
            name = "Promise to Delivery",
            description = "Project execution from commitment through customer acceptance",
            packIds = listOf("pack-delivery"),
            jtbdSequence = listOf(
                "jtbd-make-promise",
                "jtbd-execute-work",
                "jtbd-confirm-acceptance",
            ),
            icon = "shippingbox",
        ),
        Blueprint(
            id = "blueprint-hire-to-productive",
            name = "Hire to Productive",
            description = "New hire onboarding through first contribution",
            packIds = listOf("pack-people", "pack-delivery"),
            jtbdSequence = listOf(
                "jtbd-onboard-person",
                "jtbd-grant-access",
                "jtbd-execute-work",
            ),
            icon = "person.badge.plus",
        ),
        Blueprint(
            id = "blueprint-compliance-cycle",
            name = "Compliance Cycle",
            description = "Period close with full audit trail and compliance recording",
            packIds = listOf("pack-money", "pack-trust"),
            jtbdSequence = listOf(
                "jtbd-reconcile-books",
                "jtbd-close-period",
                "jtbd-record-compliance",
            ),
            icon = "checkmark.shield",
        ),
    )

    // ================================
    // FLOWS (Event-Driven Cross-Pack)
    // ================================

    val flows: List<DomainFlow> = listOf(
        DomainFlow(
            id = "flow-deal-closed",
            name = "Deal Closed",
            trigger = FlowTrigger(
                artifactType = ArtifactType.DEAL,
                event = TriggerEvent.CREATED,
            ),
            steps = listOf(
                FlowStep(id = "step-1", jtbdId = "jtbd-make-promise", condition = null, isOptional = false),
                FlowStep(
                    id = "step-2",
                    jtbdId = "jtbd-onboard-person",
                    condition = "requiresNewHire",
                    isOptional = true,
                ),
            ),
        ),
        DomainFlow(
            id = "flow-work-completed",
            name = "Work Completed",
            trigger = FlowTrigger(
                artifactType = ArtifactType.WORK_ITEM,
                event = TriggerEvent.COMPLETED,
            ),
            steps = listOf(
                FlowStep(id = "step-1", jtbdId = "jtbd-confirm-acceptance", condition = null, isOptional = false),
                FlowStep(id = "step-2", jtbdId = "jtbd-issue-invoice", condition = null, isOptional = false),
            ),
        ),
        DomainFlow(
            id = "flow-payment-received",
            name = "Payment Received",
            trigger = FlowTrigger(
                artifactType = ArtifactType.PAYMENT,
                event = TriggerEvent.CREATED,
            ),
            steps = listOf(
                FlowStep(id = "step-1", jtbdId = "jtbd-reconcile-books", condition = null, isOptional = false),
            ),
        ),
        DomainFlow(
            id = "flow-blocker-raised",
            name = "Blocker Raised",
            trigger = FlowTrigger(
                artifactType = ArtifactType.BLOCKER,
                event = TriggerEvent.CREATED,
            ),
            steps = listOf(
                FlowStep(id = "step-1", jtbdId = "jtbd-resolve-blocker", condition = null, isOptional = false),
            ),
        ),
        DomainFlow(
            id = "flow-period-end",
            name = "Period End",
            trigger = FlowTrigger(
                artifactType = ArtifactType.RECONCILIATION,
                event = TriggerEvent.COMPLETED,
            ),
            steps = listOf(
                FlowStep(id = "step-1", jtbdId = "jtbd-close-period", condition = null, isOptional = false),
                FlowStep(
                    id = "step-2",
                    jtbdId = "jtbd-record-compliance",
                    condition = "requiresCompliance",
                    isOptional = true,
                ),
            ),
        ),
    )

    // ================================
    // LOOKUP METHODS
    // ================================

    fun getPack(id: String): DomainPack? = packs.find { it.id == id }

    fun getJTBD(id: String): JTBD? = jtbds[id]

    fun getBlueprint(id: String): Blueprint? = blueprints.find { it.id == id }

    fun getFlow(id: String): DomainFlow? = flows.find { it.id == id }

    fun getJTBDsForPack(packId: String): List<JTBD> = jtbds.values.filter { it.packId == packId }

    fun getNextJTBDs(currentJtbdId: String): List<JTBD> {
        val current = jtbds[currentJtbdId] ?: return emptyList()
        return current.nextSteps.mapNotNull { jtbds[it] }
    }

    fun getJTBDsProducingArtifact(artifactType: ArtifactType): List<JTBD> =
        jtbds.values.filter { artifactType in it.producedArtifacts }

    fun getJTBDsRequiringArtifact(artifactType: ArtifactType): List<JTBD> =
        jtbds.values.filter { artifactType in it.requiredArtifacts }

    fun getRelatedPacks(packId: String): List<DomainPack> {
        val pack = getPack(packId) ?: return emptyList()
        return pack.relatedPacks.mapNotNull { getPack(it) }
    }

    /**
     * Suggest next JTBDs based on recently produced artifacts.
     * Artifact flow: if you produced X, here's what uses X.
     */
    fun suggestFromArtifacts(producedArtifacts: Set<ArtifactType>): List<JTBD> {
        return producedArtifacts.flatMap { artifact ->
            getJTBDsRequiringArtifact(artifact)
        }.distinctBy { it.id }
    }

    /**
     * Suggest JTBDs based on active blueprint progress.
     */
    fun suggestFromBlueprint(blueprintId: String, completedJtbds: Set<String>): JTBD? {
        val blueprint = getBlueprint(blueprintId) ?: return null
        return blueprint.jtbdSequence
            .firstOrNull { it !in completedJtbds }
            ?.let { jtbds[it] }
    }

    /**
     * Get pending prerequisites for a JTBD.
     */
    fun getPendingPrerequisites(jtbdId: String, completedJtbds: Set<String>): List<JTBD> {
        val jtbd = getJTBD(jtbdId) ?: return emptyList()
        return jtbd.prerequisites
            .filter { it !in completedJtbds }
            .mapNotNull { jtbds[it] }
    }

    /**
     * Suggest next JTBDs after completing a job.
     */
    fun getSuggestedNextJTBDs(afterJtbdId: String, completedJtbds: Set<String>): List<JTBD> {
        val jtbd = getJTBD(afterJtbdId) ?: return emptyList()

        // Priority 1: Explicit next steps
        val nextSteps = jtbd.nextSteps.mapNotNull { jtbds[it] }

        // Priority 2: JTBDs that consume the artifacts we just produced
        val consumingJTBDs = jtbd.producedArtifacts.flatMap { artifact ->
            getJTBDsRequiringArtifact(artifact)
        }.filter { it.id !in completedJtbds }

        // Combine and dedupe
        val seen = mutableSetOf<String>()
        val result = mutableListOf<JTBD>()

        for (j in nextSteps + consumingJTBDs) {
            if (j.id !in seen) {
                seen.add(j.id)
                result.add(j)
            }
        }

        return result
    }
}

/**
 * Domain Pack - a business capability area.
 */
data class DomainPack(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val relatedPacks: List<String>,
)

/**
 * Job To Be Done - outcome-focused user goal.
 * Matches iOS structure with verb/object for display name generation.
 */
data class JTBD(
    val id: String,
    val packId: String,
    val name: String,
    val verb: String,
    val obj: String, // "object" is reserved in Kotlin
    val outcome: String,
    val prerequisites: List<String>,
    val nextSteps: List<String>,
    val requiredArtifacts: List<ArtifactType>,
    val producedArtifacts: List<ArtifactType>,
) {
    val displayName: String get() = "$verb $obj"
}

/**
 * Blueprint - multi-pack workflow with JTBD sequence.
 */
data class Blueprint(
    val id: String,
    val name: String,
    val description: String,
    val packIds: List<String>,
    val jtbdSequence: List<String>,
    val icon: String,
) {
    fun progress(completedJTBDs: Set<String>): Double {
        if (jtbdSequence.isEmpty()) return 0.0
        val completed = jtbdSequence.count { it in completedJTBDs }
        return completed.toDouble() / jtbdSequence.size
    }

    fun nextJTBD(completedJTBDs: Set<String>): String? {
        return jtbdSequence.firstOrNull { it !in completedJTBDs }
    }
}

/**
 * Domain Flow - event-driven cross-pack trigger.
 */
data class DomainFlow(
    val id: String,
    val name: String,
    val trigger: FlowTrigger,
    val steps: List<FlowStep>,
)

data class FlowTrigger(
    val artifactType: ArtifactType,
    val event: TriggerEvent,
)

enum class TriggerEvent {
    CREATED,
    COMPLETED,
    FAILED,
    APPROVED,
    REJECTED,
}

data class FlowStep(
    val id: String,
    val jtbdId: String,
    val condition: String?,
    val isOptional: Boolean,
)

/**
 * Artifact types across all packs.
 * Matches iOS ArtifactType enum.
 */
enum class ArtifactType {
    // Money Pack
    INVOICE,
    PAYMENT,
    RECONCILIATION,
    PERIOD_CLOSE,

    // Customers Pack
    LEAD,
    OPPORTUNITY,
    PROPOSAL,
    CONTRACT,
    DEAL,

    // Delivery Pack
    PROMISE,
    WORK_ITEM,
    BLOCKER,
    ACCEPTANCE,
    POST_MORTEM,

    // People Pack
    EMPLOYEE,
    CONTRACTOR,
    ACCESS_GRANT,
    PAYROLL_RUN,

    // Trust Pack
    AUDIT_ENTRY,
    COMPLIANCE_RECORD,

    // Generic
    SIGNAL,
    STRATEGY,
    EVALUATION,
    DECISION,
    ;

    val displayName: String get() = when (this) {
        INVOICE -> "Invoice"
        PAYMENT -> "Payment"
        RECONCILIATION -> "Reconciliation"
        PERIOD_CLOSE -> "Period Close"
        LEAD -> "Lead"
        OPPORTUNITY -> "Opportunity"
        PROPOSAL -> "Proposal"
        CONTRACT -> "Contract"
        DEAL -> "Deal"
        PROMISE -> "Promise"
        WORK_ITEM -> "Work Item"
        BLOCKER -> "Blocker"
        ACCEPTANCE -> "Acceptance"
        POST_MORTEM -> "Post-Mortem"
        EMPLOYEE -> "Employee"
        CONTRACTOR -> "Contractor"
        ACCESS_GRANT -> "Access Grant"
        PAYROLL_RUN -> "Payroll Run"
        AUDIT_ENTRY -> "Audit Entry"
        COMPLIANCE_RECORD -> "Compliance Record"
        SIGNAL -> "Signal"
        STRATEGY -> "Strategy"
        EVALUATION -> "Evaluation"
        DECISION -> "Decision"
    }
}
