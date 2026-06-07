// Top-level build file.
//
// NOTE: Plugins are intentionally NOT declared here (not even with `apply false`).
// Declaring the Hilt and KSP plugins at the root made Gradle load them in the
// root classloader while KSP (applied in :app) loaded in a child classloader,
// causing "The KSP plugin was detected to be applied but its task class could not
// be found" (Dagger issue #3965) under Gradle 9.4.x / AGP 9.x.
//
// For this single-module project the :app plugins {} block resolves every plugin
// via the version catalog + settings.gradle.kts pluginManagement, keeping Hilt and
// KSP in the same classloader.
