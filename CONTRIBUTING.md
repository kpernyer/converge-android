# Contributing to Converge Android

Thank you for your interest in contributing to Converge Android!

## Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/kpernyer/converge-android.git
   cd converge-android
   ```

2. Open in Android Studio (Hedgehog or later)

3. Sync Gradle and build:
   ```bash
   ./gradlew assembleDebug
   ```

## Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Compose best practices for UI code
- Keep functions small and focused
- Write self-documenting code with clear names

## Architecture Guidelines

- **UI Layer**: Jetpack Compose with Material 3
- **ViewModel**: Use StateFlow for reactive state
- **Domain**: Keep domain models in `data/` package
- **ML**: TensorFlow Lite models in `ml/` package

## Pull Request Process

1. Create a feature branch from `main`
2. Make your changes with clear commit messages
3. Ensure the build passes: `./gradlew build`
4. Run tests: `./gradlew test`
5. Update documentation if needed
6. Submit a pull request

## Commit Messages

Use conventional commits:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `refactor:` Code refactoring
- `test:` Test additions/changes
- `chore:` Maintenance tasks

## Domain Model Changes

When modifying domain models (`DomainKnowledgeBase.kt`, `Domain.kt`):
- Ensure consistency with iOS app ([converge-ios](https://github.com/kpernyer/converge-ios))
- Follow ID conventions: `pack-`, `jtbd-`, `blueprint-`, `flow-`
- Update related documentation

## Questions?

Open an issue for discussion or reach out to the maintainers.
