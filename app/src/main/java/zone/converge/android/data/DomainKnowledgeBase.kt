// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android.data

/**
 * Domain Knowledge Base for Converge.
 *
 * Contains the canonical domain model:
 * - 5 Core Packs: Money, Customers, Delivery, People, Trust
 * - 15+ Core JTBDs (Jobs To Be Done)
 * - 5 Blueprints: Lead-to-Cash, Invoice-to-Cash, Promise-to-Delivery, etc.
 * - 5 Flows: Deal-Closed, Work-Completed, Payment-Received, etc.
 *
 * This is derived from converge-business research.
 */
object DomainKnowledgeBase {

    // ================================
    // PACKS
    // ================================

    val packs: List<DomainPack> = listOf(
        DomainPack(
            id = "money",
            name = "Money",
            description = "AR → AP → Reconcile → Close",
            jtbds = listOf("issue-invoice", "receive-payment", "pay-vendor", "reconcile-account", "close-period"),
            icon = "attach_money",
        ),
        DomainPack(
            id = "customers",
            name = "Customers",
            description = "Lead → Qualify → Offer → Close → Handoff",
            jtbds = listOf("capture-lead", "qualify-lead", "send-proposal", "close-deal", "handoff-customer"),
            icon = "people",
        ),
        DomainPack(
            id = "delivery",
            name = "Delivery",
            description = "Promise → Execute → Complete",
            jtbds = listOf("make-promise", "track-execution", "complete-delivery", "handle-blocker"),
            icon = "local_shipping",
        ),
        DomainPack(
            id = "people",
            name = "People",
            description = "Hire → Onboard → Pay → Review → Offboard",
            jtbds = listOf("post-job", "hire-candidate", "onboard-employee", "run-payroll", "conduct-review"),
            icon = "badge",
        ),
        DomainPack(
            id = "trust",
            name = "Trust",
            description = "Identity → Access → Audit → Compliance",
            jtbds = listOf("verify-identity", "grant-access", "audit-action", "check-compliance"),
            icon = "verified_user",
        ),
    )

    // ================================
    // JOBS TO BE DONE (JTBDs)
    // ================================

    val jtbds: Map<String, JTBD> = mapOf(
        // Money Pack
        "issue-invoice" to JTBD(
            id = "issue-invoice",
            packId = "money",
            title = "Issue Invoice",
            outcome = "Customer receives accurate invoice with correct terms",
            prerequisites = listOf("close-deal", "make-promise"),
            nextSteps = listOf("receive-payment"),
            producesArtifacts = listOf(ArtifactType.INVOICE),
            requiresArtifacts = listOf(ArtifactType.CONTRACT, ArtifactType.PROPOSAL),
        ),
        "receive-payment" to JTBD(
            id = "receive-payment",
            packId = "money",
            title = "Receive Payment",
            outcome = "Payment is recorded and matched to invoice",
            prerequisites = listOf("issue-invoice"),
            nextSteps = listOf("reconcile-account"),
            producesArtifacts = listOf(ArtifactType.PAYMENT_RECORD),
            requiresArtifacts = listOf(ArtifactType.INVOICE),
        ),
        "pay-vendor" to JTBD(
            id = "pay-vendor",
            packId = "money",
            title = "Pay Vendor",
            outcome = "Vendor payment is approved and processed",
            prerequisites = listOf("complete-delivery"),
            nextSteps = listOf("reconcile-account"),
            producesArtifacts = listOf(ArtifactType.PAYMENT_RECORD),
            requiresArtifacts = listOf(ArtifactType.INVOICE),
        ),
        "reconcile-account" to JTBD(
            id = "reconcile-account",
            packId = "money",
            title = "Reconcile Account",
            outcome = "All transactions are matched and verified",
            prerequisites = listOf("receive-payment", "pay-vendor"),
            nextSteps = listOf("close-period"),
            producesArtifacts = listOf(ArtifactType.RECONCILIATION_REPORT),
            requiresArtifacts = listOf(ArtifactType.PAYMENT_RECORD),
        ),
        "close-period" to JTBD(
            id = "close-period",
            packId = "money",
            title = "Close Period",
            outcome = "Financial period is closed with audit trail",
            prerequisites = listOf("reconcile-account"),
            nextSteps = emptyList(),
            producesArtifacts = listOf(ArtifactType.PERIOD_CLOSE_REPORT),
            requiresArtifacts = listOf(ArtifactType.RECONCILIATION_REPORT),
        ),

        // Customers Pack
        "capture-lead" to JTBD(
            id = "capture-lead",
            packId = "customers",
            title = "Capture Lead",
            outcome = "Lead is enriched, scored, and assigned within 24h",
            prerequisites = emptyList(),
            nextSteps = listOf("qualify-lead"),
            producesArtifacts = listOf(ArtifactType.LEAD_RECORD),
            requiresArtifacts = emptyList(),
        ),
        "qualify-lead" to JTBD(
            id = "qualify-lead",
            packId = "customers",
            title = "Qualify Lead",
            outcome = "Lead is scored and routed to appropriate rep",
            prerequisites = listOf("capture-lead"),
            nextSteps = listOf("send-proposal"),
            producesArtifacts = listOf(ArtifactType.QUALIFICATION_SCORE),
            requiresArtifacts = listOf(ArtifactType.LEAD_RECORD),
        ),
        "send-proposal" to JTBD(
            id = "send-proposal",
            packId = "customers",
            title = "Send Proposal",
            outcome = "Proposal is sent with tracked opens and engagement",
            prerequisites = listOf("qualify-lead"),
            nextSteps = listOf("close-deal"),
            producesArtifacts = listOf(ArtifactType.PROPOSAL),
            requiresArtifacts = listOf(ArtifactType.QUALIFICATION_SCORE),
        ),
        "close-deal" to JTBD(
            id = "close-deal",
            packId = "customers",
            title = "Close Deal",
            outcome = "Contract is signed and deal is closed-won",
            prerequisites = listOf("send-proposal"),
            nextSteps = listOf("handoff-customer", "issue-invoice"),
            producesArtifacts = listOf(ArtifactType.CONTRACT, ArtifactType.DEAL_RECORD),
            requiresArtifacts = listOf(ArtifactType.PROPOSAL),
        ),
        "handoff-customer" to JTBD(
            id = "handoff-customer",
            packId = "customers",
            title = "Handoff Customer",
            outcome = "Customer is handed off to delivery with clear expectations",
            prerequisites = listOf("close-deal"),
            nextSteps = listOf("make-promise"),
            producesArtifacts = listOf(ArtifactType.HANDOFF_DOCUMENT),
            requiresArtifacts = listOf(ArtifactType.CONTRACT),
        ),

        // Delivery Pack
        "make-promise" to JTBD(
            id = "make-promise",
            packId = "delivery",
            title = "Make Promise",
            outcome = "Delivery scope and timeline are committed",
            prerequisites = listOf("handoff-customer"),
            nextSteps = listOf("track-execution"),
            producesArtifacts = listOf(ArtifactType.PROMISE_RECORD),
            requiresArtifacts = listOf(ArtifactType.HANDOFF_DOCUMENT, ArtifactType.CONTRACT),
        ),
        "track-execution" to JTBD(
            id = "track-execution",
            packId = "delivery",
            title = "Track Execution",
            outcome = "Delivery progress is visible and on track",
            prerequisites = listOf("make-promise"),
            nextSteps = listOf("complete-delivery", "handle-blocker"),
            producesArtifacts = listOf(ArtifactType.STATUS_REPORT),
            requiresArtifacts = listOf(ArtifactType.PROMISE_RECORD),
        ),
        "complete-delivery" to JTBD(
            id = "complete-delivery",
            packId = "delivery",
            title = "Complete Delivery",
            outcome = "Delivery is completed and accepted by customer",
            prerequisites = listOf("track-execution"),
            nextSteps = listOf("issue-invoice"),
            producesArtifacts = listOf(ArtifactType.COMPLETION_CERTIFICATE),
            requiresArtifacts = listOf(ArtifactType.PROMISE_RECORD),
        ),
        "handle-blocker" to JTBD(
            id = "handle-blocker",
            packId = "delivery",
            title = "Handle Blocker",
            outcome = "Blocker is resolved or escalated appropriately",
            prerequisites = listOf("track-execution"),
            nextSteps = listOf("track-execution"),
            producesArtifacts = listOf(ArtifactType.ESCALATION_RECORD),
            requiresArtifacts = listOf(ArtifactType.STATUS_REPORT),
        ),

        // People Pack
        "post-job" to JTBD(
            id = "post-job",
            packId = "people",
            title = "Post Job",
            outcome = "Job posting is live and attracting qualified candidates",
            prerequisites = emptyList(),
            nextSteps = listOf("hire-candidate"),
            producesArtifacts = listOf(ArtifactType.JOB_POSTING),
            requiresArtifacts = emptyList(),
        ),
        "hire-candidate" to JTBD(
            id = "hire-candidate",
            packId = "people",
            title = "Hire Candidate",
            outcome = "Candidate accepts offer and is ready to start",
            prerequisites = listOf("post-job"),
            nextSteps = listOf("onboard-employee"),
            producesArtifacts = listOf(ArtifactType.OFFER_LETTER, ArtifactType.EMPLOYEE_RECORD),
            requiresArtifacts = listOf(ArtifactType.JOB_POSTING),
        ),
        "onboard-employee" to JTBD(
            id = "onboard-employee",
            packId = "people",
            title = "Onboard Employee",
            outcome = "Employee is productive with access to all systems",
            prerequisites = listOf("hire-candidate"),
            nextSteps = listOf("run-payroll", "grant-access"),
            producesArtifacts = listOf(ArtifactType.ONBOARDING_CHECKLIST),
            requiresArtifacts = listOf(ArtifactType.EMPLOYEE_RECORD),
        ),
        "run-payroll" to JTBD(
            id = "run-payroll",
            packId = "people",
            title = "Run Payroll",
            outcome = "Employees are paid accurately and on time",
            prerequisites = listOf("onboard-employee"),
            nextSteps = listOf("reconcile-account"),
            producesArtifacts = listOf(ArtifactType.PAYROLL_RECORD),
            requiresArtifacts = listOf(ArtifactType.EMPLOYEE_RECORD),
        ),
        "conduct-review" to JTBD(
            id = "conduct-review",
            packId = "people",
            title = "Conduct Review",
            outcome = "Performance is documented with clear feedback",
            prerequisites = emptyList(),
            nextSteps = emptyList(),
            producesArtifacts = listOf(ArtifactType.REVIEW_DOCUMENT),
            requiresArtifacts = listOf(ArtifactType.EMPLOYEE_RECORD),
        ),

        // Trust Pack
        "verify-identity" to JTBD(
            id = "verify-identity",
            packId = "trust",
            title = "Verify Identity",
            outcome = "Identity is verified with appropriate evidence",
            prerequisites = emptyList(),
            nextSteps = listOf("grant-access"),
            producesArtifacts = listOf(ArtifactType.IDENTITY_VERIFICATION),
            requiresArtifacts = emptyList(),
        ),
        "grant-access" to JTBD(
            id = "grant-access",
            packId = "trust",
            title = "Grant Access",
            outcome = "Access is granted with proper authorization",
            prerequisites = listOf("verify-identity"),
            nextSteps = listOf("audit-action"),
            producesArtifacts = listOf(ArtifactType.ACCESS_GRANT),
            requiresArtifacts = listOf(ArtifactType.IDENTITY_VERIFICATION),
        ),
        "audit-action" to JTBD(
            id = "audit-action",
            packId = "trust",
            title = "Audit Action",
            outcome = "Action is logged with full provenance",
            prerequisites = emptyList(),
            nextSteps = listOf("check-compliance"),
            producesArtifacts = listOf(ArtifactType.AUDIT_LOG),
            requiresArtifacts = emptyList(),
        ),
        "check-compliance" to JTBD(
            id = "check-compliance",
            packId = "trust",
            title = "Check Compliance",
            outcome = "Compliance status is verified and documented",
            prerequisites = listOf("audit-action"),
            nextSteps = emptyList(),
            producesArtifacts = listOf(ArtifactType.COMPLIANCE_REPORT),
            requiresArtifacts = listOf(ArtifactType.AUDIT_LOG),
        ),
    )

    // ================================
    // BLUEPRINTS (Multi-Pack Workflows)
    // ================================

    val blueprints: List<Blueprint> = listOf(
        Blueprint(
            id = "lead-to-cash",
            name = "Lead to Cash",
            description = "Complete customer lifecycle from first contact to payment",
            packs = listOf("customers", "delivery", "money"),
            jtbdSequence = listOf(
                "capture-lead", "qualify-lead", "send-proposal", "close-deal",
                "handoff-customer", "make-promise", "track-execution",
                "complete-delivery", "issue-invoice", "receive-payment",
            ),
        ),
        Blueprint(
            id = "invoice-to-cash",
            name = "Invoice to Cash",
            description = "From invoice creation to cash collection",
            packs = listOf("money"),
            jtbdSequence = listOf("issue-invoice", "receive-payment", "reconcile-account"),
        ),
        Blueprint(
            id = "promise-to-delivery",
            name = "Promise to Delivery",
            description = "From commitment to successful completion",
            packs = listOf("delivery"),
            jtbdSequence = listOf("make-promise", "track-execution", "complete-delivery"),
        ),
        Blueprint(
            id = "hire-to-productive",
            name = "Hire to Productive",
            description = "From job posting to productive employee",
            packs = listOf("people", "trust"),
            jtbdSequence = listOf(
                "post-job", "hire-candidate", "verify-identity",
                "grant-access", "onboard-employee",
            ),
        ),
        Blueprint(
            id = "compliance-cycle",
            name = "Compliance Cycle",
            description = "Regular compliance verification and reporting",
            packs = listOf("trust", "money"),
            jtbdSequence = listOf("audit-action", "check-compliance", "close-period"),
        ),
    )

    // ================================
    // FLOWS (Event-Driven Cross-Pack)
    // ================================

    val flows: List<DomainFlow> = listOf(
        DomainFlow(
            id = "deal-closed",
            name = "Deal Closed",
            description = "Trigger when a deal is closed-won",
            triggerEvent = "deal.closed_won",
            triggerPack = "customers",
            targetJtbds = listOf("handoff-customer", "issue-invoice", "make-promise"),
        ),
        DomainFlow(
            id = "work-completed",
            name = "Work Completed",
            description = "Trigger when delivery is completed",
            triggerEvent = "delivery.completed",
            triggerPack = "delivery",
            targetJtbds = listOf("issue-invoice", "receive-payment"),
        ),
        DomainFlow(
            id = "payment-received",
            name = "Payment Received",
            description = "Trigger when payment is received",
            triggerEvent = "payment.received",
            triggerPack = "money",
            targetJtbds = listOf("reconcile-account"),
        ),
        DomainFlow(
            id = "blocker-raised",
            name = "Blocker Raised",
            description = "Trigger when a blocker is identified",
            triggerEvent = "delivery.blocked",
            triggerPack = "delivery",
            targetJtbds = listOf("handle-blocker"),
        ),
        DomainFlow(
            id = "period-end",
            name = "Period End",
            description = "Trigger at end of financial period",
            triggerEvent = "calendar.period_end",
            triggerPack = "money",
            targetJtbds = listOf("reconcile-account", "close-period", "check-compliance"),
        ),
    )

    // ================================
    // LOOKUP METHODS
    // ================================

    fun getPack(id: String): DomainPack? = packs.find { it.id == id }

    fun getJTBD(id: String): JTBD? = jtbds[id]

    fun getBlueprint(id: String): Blueprint? = blueprints.find { it.id == id }

    fun getFlow(id: String): DomainFlow? = flows.find { it.id == id }

    fun getJTBDsForPack(packId: String): List<JTBD> =
        jtbds.values.filter { it.packId == packId }

    fun getNextJTBDs(currentJtbdId: String): List<JTBD> {
        val current = jtbds[currentJtbdId] ?: return emptyList()
        return current.nextSteps.mapNotNull { jtbds[it] }
    }

    fun getJTBDsProducingArtifact(artifactType: ArtifactType): List<JTBD> =
        jtbds.values.filter { artifactType in it.producesArtifacts }

    fun getJTBDsRequiringArtifact(artifactType: ArtifactType): List<JTBD> =
        jtbds.values.filter { artifactType in it.requiresArtifacts }

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
}

/**
 * Domain Pack - a business capability area.
 */
data class DomainPack(
    val id: String,
    val name: String,
    val description: String,
    val jtbds: List<String>,
    val icon: String,
)

/**
 * Job To Be Done - outcome-focused user goal.
 */
data class JTBD(
    val id: String,
    val packId: String,
    val title: String,
    val outcome: String,
    val prerequisites: List<String>,
    val nextSteps: List<String>,
    val producesArtifacts: List<ArtifactType>,
    val requiresArtifacts: List<ArtifactType>,
)

/**
 * Blueprint - multi-pack workflow with JTBD sequence.
 */
data class Blueprint(
    val id: String,
    val name: String,
    val description: String,
    val packs: List<String>,
    val jtbdSequence: List<String>,
)

/**
 * Domain Flow - event-driven cross-pack trigger.
 */
data class DomainFlow(
    val id: String,
    val name: String,
    val description: String,
    val triggerEvent: String,
    val triggerPack: String,
    val targetJtbds: List<String>,
)

/**
 * Artifact types across all packs.
 */
enum class ArtifactType {
    // Money
    INVOICE, PAYMENT_RECORD, RECONCILIATION_REPORT, PERIOD_CLOSE_REPORT,

    // Customers
    LEAD_RECORD, QUALIFICATION_SCORE, PROPOSAL, CONTRACT, DEAL_RECORD, HANDOFF_DOCUMENT,

    // Delivery
    PROMISE_RECORD, STATUS_REPORT, COMPLETION_CERTIFICATE, ESCALATION_RECORD,

    // People
    JOB_POSTING, OFFER_LETTER, EMPLOYEE_RECORD, ONBOARDING_CHECKLIST, PAYROLL_RECORD, REVIEW_DOCUMENT,

    // Trust
    IDENTITY_VERIFICATION, ACCESS_GRANT, AUDIT_LOG, COMPLIANCE_REPORT,
}
