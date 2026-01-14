# AGENTS.md — Converge Android Guide

> Converge is a vision for **semantic governance**. We move from fragmented intent to unified, converged states through a deterministic alignment engine. Our mission is to provide a stable foundation for complex decision-making where human authority and AI agency coexist in a transparent, explainable ecosystem.

**For AI coding assistants (Claude, Gemini, Codex, Cursor, etc.)**

This repository contains the **Android mobile application** for Converge.

## Project Specifics

- **Kotlin & Compose**: Modern Android development with Jetpack Compose.
- **On-Device ML**: Uses TFLite for local action prediction.
- **gRPC Kotlin**: Bidirectional streaming for runtime interaction.

## Development Patterns

- **MVVM Architecture**: Strict separation of concerns between UI, ViewModel, and Domain.
- **State Management**: Use `StateFlow` and `collectAsStateWithLifecycle`.
- **UI Consistency**: Follow the Converge Design System (colors, typography).
- **Cross-Platform**: Adhere to the [android-CROSS_PLATFORM_CONTRACT.md](../converge-business/knowledgebase/android-CROSS_PLATFORM_CONTRACT.md).

---

## Consolidated Documentation (converge-business)

- **Knowledgebase**: [converge-business/knowledgebase/](../converge-business/knowledgebase/)
- **Cross-Platform Contract**: [converge-business/knowledgebase/android-CROSS_PLATFORM_CONTRACT.md](../converge-business/knowledgebase/android-CROSS_PLATFORM_CONTRACT.md)
