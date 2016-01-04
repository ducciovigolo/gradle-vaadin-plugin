package fi.jasoft.plugin.tasks

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.invocation.Gradle

class ResourceCleaner extends BuildAdapter {

    Closure callback

    static void onBuildFinished(Gradle gradle, def callback) {
        gradle.addBuildListener(new ResourceCleaner(callback: callback))
    }

    @Override
    void buildFinished(BuildResult result) {
        callback.call(result)
    }
}
