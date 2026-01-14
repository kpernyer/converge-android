// Copyright 2024-2025 Aprio One AB, Sweden
// Author: Kenneth Pernyer, kenneth@aprio.one
// SPDX-License-Identifier: MIT

package zone.converge.android.data

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DomainKnowledgeBase")
class DomainKnowledgeBaseTest {

    @Nested
    @DisplayName("Packs")
    inner class Packs {

        @Test
        @DisplayName("should have exactly 5 core packs")
        fun hasFiveCorePacks() {
            assertThat(DomainKnowledgeBase.packs).hasSize(5)
        }

        @Test
        @DisplayName("should contain Money pack")
        fun containsMoneyPack() {
            val pack = DomainKnowledgeBase.getPack("pack-money")
            assertThat(pack).isNotNull()
            assertThat(pack!!.name).isEqualTo("Money")
        }

        @Test
        @DisplayName("should contain Customers pack")
        fun containsCustomersPack() {
            val pack = DomainKnowledgeBase.getPack("pack-customers")
            assertThat(pack).isNotNull()
            assertThat(pack!!.name).isEqualTo("Customers")
        }

        @Test
        @DisplayName("should contain Delivery pack")
        fun containsDeliveryPack() {
            val pack = DomainKnowledgeBase.getPack("pack-delivery")
            assertThat(pack).isNotNull()
            assertThat(pack!!.name).isEqualTo("Delivery")
        }

        @Test
        @DisplayName("should contain People pack")
        fun containsPeoplePack() {
            val pack = DomainKnowledgeBase.getPack("pack-people")
            assertThat(pack).isNotNull()
            assertThat(pack!!.name).isEqualTo("People")
        }

        @Test
        @DisplayName("should contain Trust pack")
        fun containsTrustPack() {
            val pack = DomainKnowledgeBase.getPack("pack-trust")
            assertThat(pack).isNotNull()
            assertThat(pack!!.name).isEqualTo("Trust")
        }

        @Test
        @DisplayName("should return null for unknown pack")
        fun returnsNullForUnknownPack() {
            assertThat(DomainKnowledgeBase.getPack("pack-unknown")).isNull()
        }

        @Test
        @DisplayName("each pack should have related packs")
        fun packsHaveRelatedPacks() {
            DomainKnowledgeBase.packs.forEach { pack ->
                assertThat(pack.relatedPacks).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("JTBDs")
    inner class JTBDs {

        @Test
        @DisplayName("should have 16 core JTBDs")
        fun hasSixteenJtbds() {
            assertThat(DomainKnowledgeBase.jtbds).hasSize(16)
        }

        @Test
        @DisplayName("should return JTBD by ID")
        fun returnsJtbdById() {
            val jtbd = DomainKnowledgeBase.getJTBD("jtbd-issue-invoice")
            assertThat(jtbd).isNotNull()
            assertThat(jtbd!!.name).isEqualTo("Issue Invoice")
        }

        @Test
        @DisplayName("should return null for unknown JTBD")
        fun returnsNullForUnknownJtbd() {
            assertThat(DomainKnowledgeBase.getJTBD("jtbd-unknown")).isNull()
        }

        @Test
        @DisplayName("all JTBDs should have valid pack references")
        fun jtbdsHaveValidPackReferences() {
            DomainKnowledgeBase.jtbds.values.forEach { jtbd ->
                val pack = DomainKnowledgeBase.getPack(jtbd.packId)
                assertThat(pack).isNotNull()
            }
        }

        @Test
        @DisplayName("all JTBDs should have non-empty outcome")
        fun jtbdsHaveOutcome() {
            DomainKnowledgeBase.jtbds.values.forEach { jtbd ->
                assertThat(jtbd.outcome).isNotEmpty()
            }
        }

        @Test
        @DisplayName("displayName should be verb + object")
        fun displayNameIsVerbObject() {
            val jtbd = DomainKnowledgeBase.getJTBD("jtbd-issue-invoice")!!
            assertThat(jtbd.displayName).isEqualTo("Issue invoice")
        }
    }

    @Nested
    @DisplayName("Blueprints")
    inner class Blueprints {

        @Test
        @DisplayName("should have 5 blueprints")
        fun hasFiveBlueprints() {
            assertThat(DomainKnowledgeBase.blueprints).hasSize(5)
        }

        @Test
        @DisplayName("should return blueprint by ID")
        fun returnsBlueprintById() {
            val blueprint = DomainKnowledgeBase.getBlueprint("blueprint-lead-to-cash")
            assertThat(blueprint).isNotNull()
            assertThat(blueprint!!.name).isEqualTo("Lead to Cash")
        }

        @Test
        @DisplayName("should return null for unknown blueprint")
        fun returnsNullForUnknownBlueprint() {
            assertThat(DomainKnowledgeBase.getBlueprint("blueprint-unknown")).isNull()
        }

        @Test
        @DisplayName("lead-to-cash should have correct JTBD sequence")
        fun leadToCashHasCorrectSequence() {
            val blueprint = DomainKnowledgeBase.getBlueprint("blueprint-lead-to-cash")!!
            assertThat(blueprint.jtbdSequence).containsExactly(
                "jtbd-capture-lead",
                "jtbd-develop-opportunity",
                "jtbd-create-proposal",
                "jtbd-close-deal",
                "jtbd-make-promise",
                "jtbd-execute-work",
                "jtbd-confirm-acceptance",
                "jtbd-issue-invoice",
                "jtbd-collect-payment",
            ).inOrder()
        }

        @Test
        @DisplayName("blueprint progress should be 0 when no JTBDs completed")
        fun progressZeroWhenNoJtbdsCompleted() {
            val blueprint = DomainKnowledgeBase.getBlueprint("blueprint-lead-to-cash")!!
            assertThat(blueprint.progress(emptySet())).isEqualTo(0.0)
        }

        @Test
        @DisplayName("blueprint progress should be 1.0 when all JTBDs completed")
        fun progressOneWhenAllCompleted() {
            val blueprint = DomainKnowledgeBase.getBlueprint("blueprint-lead-to-cash")!!
            val allJtbds = blueprint.jtbdSequence.toSet()
            assertThat(blueprint.progress(allJtbds)).isEqualTo(1.0)
        }

        @Test
        @DisplayName("blueprint nextJTBD should return first uncompleted")
        fun nextJtbdReturnsFirstUncompleted() {
            val blueprint = DomainKnowledgeBase.getBlueprint("blueprint-lead-to-cash")!!
            val completed = setOf("jtbd-capture-lead", "jtbd-develop-opportunity")
            assertThat(blueprint.nextJTBD(completed)).isEqualTo("jtbd-create-proposal")
        }

        @Test
        @DisplayName("blueprint nextJTBD should return null when all completed")
        fun nextJtbdNullWhenAllCompleted() {
            val blueprint = DomainKnowledgeBase.getBlueprint("blueprint-lead-to-cash")!!
            val allJtbds = blueprint.jtbdSequence.toSet()
            assertThat(blueprint.nextJTBD(allJtbds)).isNull()
        }
    }

    @Nested
    @DisplayName("Flows")
    inner class Flows {

        @Test
        @DisplayName("should have 5 flows")
        fun hasFiveFlows() {
            assertThat(DomainKnowledgeBase.flows).hasSize(5)
        }

        @Test
        @DisplayName("should return flow by ID")
        fun returnsFlowById() {
            val flow = DomainKnowledgeBase.getFlow("flow-deal-closed")
            assertThat(flow).isNotNull()
            assertThat(flow!!.name).isEqualTo("Deal Closed")
        }

        @Test
        @DisplayName("each flow should have at least one step")
        fun flowsHaveSteps() {
            DomainKnowledgeBase.flows.forEach { flow ->
                assertThat(flow.steps).isNotEmpty()
            }
        }
    }

    @Nested
    @DisplayName("Lookup methods")
    inner class LookupMethods {

        @Test
        @DisplayName("getJTBDsForPack should return correct JTBDs")
        fun getJtbdsForPack() {
            val moneyJtbds = DomainKnowledgeBase.getJTBDsForPack("pack-money")
            assertThat(moneyJtbds).hasSize(4)
            moneyJtbds.forEach { jtbd ->
                assertThat(jtbd.packId).isEqualTo("pack-money")
            }
        }

        @Test
        @DisplayName("getNextJTBDs should return next steps")
        fun getNextJtbds() {
            val nextJtbds = DomainKnowledgeBase.getNextJTBDs("jtbd-issue-invoice")
            assertThat(nextJtbds.map { it.id }).contains("jtbd-collect-payment")
        }

        @Test
        @DisplayName("getNextJTBDs should return empty for terminal JTBD")
        fun getNextJtbdsEmptyForTerminal() {
            val nextJtbds = DomainKnowledgeBase.getNextJTBDs("jtbd-close-period")
            assertThat(nextJtbds).isEmpty()
        }

        @Test
        @DisplayName("getRelatedPacks should return related packs")
        fun getRelatedPacks() {
            val relatedPacks = DomainKnowledgeBase.getRelatedPacks("pack-money")
            assertThat(relatedPacks).isNotEmpty()
            assertThat(relatedPacks.map { it.id }).contains("pack-customers")
        }

        @Test
        @DisplayName("suggestFromArtifacts should suggest JTBDs that need those artifacts")
        fun suggestFromArtifacts() {
            val suggestions = DomainKnowledgeBase.suggestFromArtifacts(setOf(ArtifactType.INVOICE))
            assertThat(suggestions.map { it.id }).contains("jtbd-collect-payment")
        }

        @Test
        @DisplayName("suggestFromBlueprint should return next JTBD in sequence")
        fun suggestFromBlueprint() {
            val completed = setOf("jtbd-capture-lead")
            val nextJtbd = DomainKnowledgeBase.suggestFromBlueprint("blueprint-lead-to-cash", completed)
            assertThat(nextJtbd?.id).isEqualTo("jtbd-develop-opportunity")
        }

        @Test
        @DisplayName("suggestFromBlueprint should return null for unknown blueprint")
        fun suggestFromBlueprintNullForUnknown() {
            val nextJtbd = DomainKnowledgeBase.suggestFromBlueprint("blueprint-unknown", emptySet())
            assertThat(nextJtbd).isNull()
        }
    }

    @Nested
    @DisplayName("Property tests - Invariants")
    inner class PropertyTests {

        @Test
        @DisplayName("all JTBD IDs should start with 'jtbd-'")
        fun jtbdIdsStartWithPrefix() {
            DomainKnowledgeBase.jtbds.keys.forEach { id ->
                assertThat(id).startsWith("jtbd-")
            }
        }

        @Test
        @DisplayName("all pack IDs should start with 'pack-'")
        fun packIdsStartWithPrefix() {
            DomainKnowledgeBase.packs.forEach { pack ->
                assertThat(pack.id).startsWith("pack-")
            }
        }

        @Test
        @DisplayName("all blueprint IDs should start with 'blueprint-'")
        fun blueprintIdsStartWithPrefix() {
            DomainKnowledgeBase.blueprints.forEach { blueprint ->
                assertThat(blueprint.id).startsWith("blueprint-")
            }
        }

        @Test
        @DisplayName("all flow IDs should start with 'flow-'")
        fun flowIdsStartWithPrefix() {
            DomainKnowledgeBase.flows.forEach { flow ->
                assertThat(flow.id).startsWith("flow-")
            }
        }

        @Test
        @DisplayName("all JTBD prerequisite references should be valid")
        fun jtbdPrerequisitesAreValid() {
            DomainKnowledgeBase.jtbds.values.forEach { jtbd ->
                jtbd.prerequisites.forEach { prereqId ->
                    val prereq = DomainKnowledgeBase.getJTBD(prereqId)
                    assertThat(prereq)
                        .isNotNull()
                }
            }
        }

        @Test
        @DisplayName("all JTBD nextSteps references should be valid")
        fun jtbdNextStepsAreValid() {
            DomainKnowledgeBase.jtbds.values.forEach { jtbd ->
                jtbd.nextSteps.forEach { nextId ->
                    val nextJtbd = DomainKnowledgeBase.getJTBD(nextId)
                    assertThat(nextJtbd).isNotNull()
                }
            }
        }

        @Test
        @DisplayName("all blueprint JTBD sequence references should be valid")
        fun blueprintJtbdSequencesAreValid() {
            DomainKnowledgeBase.blueprints.forEach { blueprint ->
                blueprint.jtbdSequence.forEach { jtbdId ->
                    val jtbd = DomainKnowledgeBase.getJTBD(jtbdId)
                    assertThat(jtbd).isNotNull()
                }
            }
        }

        @Test
        @DisplayName("all pack related pack references should be valid")
        fun packRelatedPacksAreValid() {
            DomainKnowledgeBase.packs.forEach { pack ->
                pack.relatedPacks.forEach { relatedId ->
                    val relatedPack = DomainKnowledgeBase.getPack(relatedId)
                    assertThat(relatedPack).isNotNull()
                }
            }
        }
    }
}
