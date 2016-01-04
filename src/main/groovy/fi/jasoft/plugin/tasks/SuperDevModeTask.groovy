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

import fi.jasoft.plugin.ApplicationServer
import fi.jasoft.plugin.DependencyListener
import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

class SuperDevModeTask extends DefaultTask {

    static final String NAME = 'vaadinSuperDevMode'

    def Process codeserverProcess = null

    def server = null

    def SuperDevModeTask() {
        dependsOn(CompileWidgetsetTask.NAME)
        description = "Run Super Development Mode for easier client widget development."

        ResourceCleaner.onBuildFinished(project.gradle, {
            if(codeserverProcess){
                codeserverProcess.destroy()
                codeserverProcess = null
            }

            if(server) {
                server.terminate()
                server = null
            }
        })
    }

    @TaskAction
    def run() {

        if (!project.vaadin.devmode.superDevMode) {
            logger.error 'SuperDevMode is a experimental feature and is not enabled for project by default. To enable it set vaadin.devmode.superDevMode to true'
            throw new GradleException("Property vaadin.devmode.superDevMode not set.")
        }

        if(!project.vaadin.widgetset) {
            logger.error 'No widgetset defined (can be set with vaadin.widgetset in build.gradle)'
            throw new GradleException("Property vaadin.widgetset not set.")
        }

        runCodeServer({

            server = new ApplicationServer(project, ['superdevmode'])

            server.startAndBlock();

            codeserverProcess.waitForOrKill(1)
        })
    }

    def runCodeServer(Closure readyClosure) {
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File widgetsetsDir = new File(webAppDir.canonicalPath + '/VAADIN/widgetsets')
        widgetsetsDir.mkdirs()

        def jettyClasspath = project.configurations[DependencyListener.Configuration.JETTY8.caption];
        def classpath = jettyClasspath + Util.getClientCompilerClassPath(project)
        def superdevmodeProcess = ['java',
            '-cp', classpath.getAsPath(),
            'com.google.gwt.dev.codeserver.CodeServer',
            '-bindAddress', project.vaadin.devmode.bindAddress,
            '-port', 9876,
            '-workDir', widgetsetsDir.canonicalPath,
            '-src', javaDir.canonicalPath,
            '-logLevel', project.vaadin.gwt.logLevel,
            '-noprecompile',
            project.vaadin.widgetset
        ]

        codeserverProcess = superdevmodeProcess.execute()

        Util.logProcess(project, codeserverProcess, 'superdevmode.log', { line ->
            if(line.contains('The code server is ready.')){
                readyClosure.call()
            }
        })

        codeserverProcess.waitFor()
    }
}

