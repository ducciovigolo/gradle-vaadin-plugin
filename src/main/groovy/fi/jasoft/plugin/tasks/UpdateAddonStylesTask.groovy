/*
* Copyright 2014 John Ahlroos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction


class UpdateAddonStylesTask extends DefaultTask {

    public static final String NAME = 'vaadinUpdateAddonStyles'

    UpdateAddonStylesTask() {
        dependsOn(BuildClassPathJar.NAME)
        description = 'Updates the addons.scss file with addon styles.'
    }

    @TaskAction
    public void run() {

        if (!Util.isAddonStylesSupported(project)) {
            return
        }

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        File themesDir = new File(webAppDir.canonicalPath + '/VAADIN/themes')
        if (!themesDir.exists()) {
            return;
        }

        FileCollection classpathFileCollection

        if(project.vaadin.plugin.useClassPathJar){
            BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
            classpathFileCollection = project.files(pathJarTask.archivePath)
        } else {
            classpathFileCollection = Util.getCompileClassPath(project)
        }

        themesDir.eachDir { dir ->
            project.logger.info("Updating ${dir.canonicalPath}/addons.scss")

            project.javaexec {
                main 'com.vaadin.server.themeutils.SASSAddonImportFileCreator'
                args dir.canonicalPath
                classpath = classpathFileCollection
            }
        }
    }
}
