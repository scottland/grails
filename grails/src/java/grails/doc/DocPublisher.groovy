/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.doc

import org.radeox.engine.context.BaseInitialRenderContext
import groovy.text.Template

/**
 * Coordinated the DocEngine the produce documentation based on the gdoc format.
 *
 * @see DocEngine
 *
 * @author Graeme Rocher
 * @since 1.2
 */
public class DocPublisher {

    /** The source directory of the documentation */
    File src
    /** The target directory to publish to */
    File target
    /** The temporary work directory */
    File workDir
    /** The directory containing any images to use (will override defaults) **/
    File images
    /** The directory containing any CSS to use (will override defaults) **/
    File css
    /** The directory cotnaining any templates to use (will override defaults) **/
    File style
    /** The AntBuilder instance to use */
    AntBuilder ant
    /** The encoding to use (default is UTF-8) */
    String encoding = "UTF-8"
    /** The title of the documentation */
    String title
    /** The subtitle of the documentation */
    String subtitle = ""
    /** The version of the documentation */
    String version
    /** The authors of the documentation */
    String authors = ""
    /** The documentation license */
    String license = ""
    /** The copyright message */
    String copyright = ""
    /** The footer to include */
    String footer = ""
    /** HTML markup that renders the left logo */
    String logo
    /** HTML markup that renders the right logo */
    String sponsorLogo
    
    /** Properties used to configure the DocEngine */
    Properties engineProperties

    DocPublisher() {
    }

    DocPublisher(File src, File target) {
        this.src = src;
        this.target = target;
    }



    /**
     * Returns the engine properties
     */
    Properties getEngineProperties() {  engineProperties }
    /**
     * Sets the engine properties
     */
    void setEngineProperties(Properties p) {
        def props = DocEngine.getDefaultProps()
        if(p) {
            props.putAll p
        }
        this.engineProperties = props
    }

    void publish() {
        initialize()
        if(src?.exists()) {
            // unpack documentation resources
            String docResources = "${workDir}/doc-resources"
            ant.mkdir(dir:docResources)
            unpack(dest: docResources, src: "grails-doc-files.jar")


            def refDocsDir = target?.absolutePath ?: "./docs/manual"
            def refGuideDir = "$refDocsDir/guide"
            def refPagesDir = "$refGuideDir/pages"

            ant.mkdir(dir:refDocsDir)
            ant.mkdir(dir:refGuideDir)
            ant.mkdir(dir:refPagesDir)
            ant.mkdir(dir:"$refDocsDir/ref")

            String imgsDir = "${refDocsDir}/img"
            ant.mkdir(dir: imgsDir)
            String cssDir = "${refDocsDir}/css"
            ant.mkdir(dir: cssDir)
            ant.mkdir(dir: "${refDocsDir}/ref")



            ant.copy(todir: imgsDir) {
                fileset(dir: "${docResources}/img")
            }
            if(images) {
                ant.copy(todir:imgsDir, overwrite:true) {
                    fileset(dir: images)
                }
            }
            ant.copy(todir: cssDir) {
                fileset(dir: "${docResources}/css")
            }
            if(css) {
                ant.copy(todir:cssDir, overwrite:true) {
                    fileset(dir: css)
                }

            }
            if(style) {
                ant.copy(todir:"${docResources}/style", overwrite:true) {
                    fileset(dir: style)
                }
            }
            ant.copy(todir: "${refDocsDir}/ref") {
                fileset(dir: "${docResources}/style/ref")
            }

     
            def comparator = [compare: {o1, o2 ->
                def idx1 = o1.name[0..o1.name.indexOf(' ') - 1]
                def idx2 = o2.name[0..o2.name.indexOf(' ') - 1]
                def nums1 = idx1.split(/\./).findAll { it.trim() != ''}*.toInteger()
                def nums2 = idx2.split(/\./).findAll { it.trim() != ''}*.toInteger()
                // pad out with zeros to ensure accurate comparison
                while (nums1.size() < nums2.size()) {
                    nums1 << 0
                }
                while (nums2.size() < nums1.size()) {
                    nums2 << 0
                }
                def result = 0
                for (i in 0..<nums1.size()) {
                    result = nums1[i].compareTo(nums2[i])
                    if (result != 0) break
                }
                result
            },
             equals: { false }] as Comparator

            def files = new File("${src}/guide").listFiles()?.findAll { it.name.endsWith(".gdoc") }?.sort(comparator) ?: []
            def context = new BaseInitialRenderContext()
            context.set(DocEngine.CONTEXT_PATH, "..")
            context.set(DocEngine.BASE_DIR, src.absolutePath) 

            def engine = new DocEngine(context)
            engine.engineProperties = engineProperties
            def templateEngine = new groovy.text.SimpleTemplateEngine()
            context.renderEngine = engine

            def book = [:]
            for (f in files) {
                def chapter = f.name[0..-6]
                book[chapter] = f
            }


            def toc = new StringBuffer()
            def soloToc = new StringBuffer()
            def fullContents = new StringBuffer()
            def chapterContents = new StringBuffer()
            def chapterTitle = null
            def vars = [
                    title: title,
                    subtitle: subtitle,
                    footer: footer, // TODO - add a way to specify footer
                    authors: authors,
                    version: version,
                    copyright: copyright,
                    toc: toc.toString(),
                    body: fullContents.toString(),
                    logo: logo,
                    sponsorLogo: sponsorLogo
            ]


            new File("${docResources}/style/guideItem.html").withReader(encoding) {reader ->
                def template = templateEngine.createTemplate(reader)

                for (entry in book) {
                    def title = entry.key
                    def level = 0
                    def matcher = (title =~ /^(\S+?)\.? /) // drops last '.' of "xx.yy. "
                    if (matcher.find()) {
                        level = matcher.group(1).split(/\./).size() - 1
                    }
                    def margin = level * 10

                    if (level == 0) {
                        if (chapterTitle) // initially null, to collect sections
                            writeChapter(template,refGuideDir, chapterTitle, chapterContents, vars)

                        chapterTitle = title // after previous used to write prev chapter

                        soloToc << "<div class=\"tocItem\" style=\"margin-left:${margin}px\"><a href=\"${chapterTitle}.html\">${chapterTitle}</a></div>"
                    }
                    else {
                        soloToc << "<div class=\"tocItem\" style=\"margin-left:${margin}px\"><a href=\"${chapterTitle}.html#${entry.key}\">${entry.key}</a></div>"
                    }        // level 0=h1, (1..n)=h2


                    def hLevel = level == 0 ? 1 : 2
                    def header = "<h$hLevel><a name=\"${title}\">${title}</a></h$hLevel>"

                    context.set(DocEngine.SOURCE_FILE, entry.value)
                    context.set(DocEngine.CONTEXT_PATH, "..")
                    def body = engine.render(entry.value.text, context)

                    toc << "<div class=\"tocItem\" style=\"margin-left:${margin}px\"><a href=\"#${title}\">${title}</a></div>"
                    fullContents << header << body
                    chapterContents << header << body

                    new File("${refPagesDir}/${title}.html").withWriter(encoding) {
                        Map args = [content: body] + vars
                        args.title = title
                        template.make(args).writeTo(it)
                    }
                }
                if (chapterTitle) // write final chapter collected (if any seen)
                    writeChapter(template,refGuideDir ,chapterTitle, chapterContents, vars)
            }


            new File("${docResources}/style/layout.html").withReader(encoding) {reader ->
                def template = templateEngine.createTemplate(reader)
                vars.toc = toc
                vars.body = fullContents.toString()
                new File("${refGuideDir}/single.html").withWriter(encoding) {out ->
                    template.make(vars).writeTo(out)
                }
                vars.toc = soloToc
                vars.body = ""
                new File("${refGuideDir}/index.html").withWriter(encoding) {out ->
                    template.make(vars).writeTo(out)
                }
            }

            new File("${docResources}/style/index.html").withReader(encoding) {reader ->
                def template = templateEngine.createTemplate(reader)
                new File("${refDocsDir}/index.html").withWriter(encoding) {out ->
                    template.make(vars).writeTo(out)
                }
            }


            def menu = new StringBuilder()
            files = new File("${src}/ref").listFiles()?.toList()?.sort() ?: []
            def reference = [:]
            new File("${docResources}/style/referenceItem.html").withReader(encoding) {reader ->
                def template = templateEngine.createTemplate(reader)
                for (f in files) {
                    if (f.directory && !f.name.startsWith(".")) {
                        def section = f.name
                        reference."${section}" = [:]
                        menu << "<h1 class=\"menuTitle\">${f.name}</h1>"
                        new File("${refDocsDir}/ref/${f.name}").mkdirs()
                        def textiles = f.listFiles().findAll { it.name.endsWith(".gdoc")}.sort()
                        def usageFile = new File("${src}/src/ref/${f.name}.gdoc")
                        if (usageFile.exists()) {
                            def data = usageFile.text
                            reference."${section}".usage = data
                            context.set(DocEngine.SOURCE_FILE, usageFile.name)
                            context.set(DocEngine.CONTEXT_PATH, "../..")
                            def contents = engine.render(data, context)
                            new File("${refDocsDir}/ref/${f.name}/Usage.html").withWriter(encoding) {out ->
                                template.make([content: contents] + vars).writeTo(out)
                            }
                            menu << "<div class=\"menuUsageItem\"><a href=\"${f.name}/Usage.html\" target=\"mainFrame\">Usage</a></div>"
                        }
                        for (txt in textiles) {
                            def name = txt.name[0..-6]
                            menu << "<div class=\"menuItem\"><a href=\"${f.name}/${name}.html\" target=\"mainFrame\">${name}</a></div>"
                            def data = txt.text
                            reference."${section}".put(name,data)
                            context.set(DocEngine.SOURCE_FILE, txt.name)
                            context.set(DocEngine.CONTEXT_PATH, "../..")
                            def contents = engine.render(data, context)
                            //println "Generating reference item: ${name}"
                            new File("${refDocsDir}/ref/${f.name}/${name}.html").withWriter(encoding) {out ->
                                template.make([content: contents] + vars).writeTo(out)
                            }
                        }
                    }
                }

            }
            vars.menu = menu
            new File("${docResources}/style/menu.html").withReader(encoding) {reader ->
                def template = templateEngine.createTemplate(reader)
                new File("${refDocsDir}/ref/menu.html").withWriter(encoding) {out ->
                    template.make(vars).writeTo(out)
                }
            }

            ant.echo "Built user manual at ${refDocsDir}/index.html"
        }
    }

    void writeChapter(Template template, String targetDir, String title, StringBuffer content, Map vars) {
            new File("${targetDir}/${title}.html").withWriter(encoding) {
                Map args = [content: content.toString()] + vars
                args.title = title
                template.make(args).writeTo(it)
            }
            content.delete(0, content.size()) // clear buffer
    }

    
    protected void initialize() {
        if (!workDir) {
            workDir = new File(System.getProperty("java.io.tmpdir"))
        }
        if (!ant) {
            ant = new AntBuilder()
        }
        def metaProps = DocPublisher.metaClass.properties
        def props = engineProperties
        for (MetaProperty mp in metaProps) {
            if (mp.type == String) {
                def value = props[mp.name]
                if(value) {
                    this[mp.name] = value
                }
            }
        }

    }

    private unpack(Map args) {

        def dir = args["dest"] ?: "."
        def src = args["src"]
        def overwriteOption = args["overwrite"] == null ? true : args["overwrite"]

        // Can't unjar a file from within a JAR, so we copy it to
        // the destination directory first.
        try {
            URL url = getClass().getClassLoader().getResource(src)
            if(url) {

                url.withInputStream { InputStream input ->
                    new File("$dir/$src").withOutputStream { out ->
                        def buffer = new byte[1024]
                        int len;
                        while ((len = input.read(buffer)) != -1) {
                            out.write(buffer, 0, len)
                        }
                    }
                }
            }
            // Now unjar it, excluding the META-INF directory.
            ant.unjar(dest: dir, src: "${dir}/${src}", overwrite: overwriteOption) {
                patternset {
                    exclude(name: "META-INF/**")
                }
            }


        }
        finally {
            // Don't need the JAR file any more, so remove it.
            ant.delete(file: "${dir}/${src}", failonerror:false)
        }

    }
}
