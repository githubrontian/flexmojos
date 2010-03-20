/**
 * Flexmojos is a set of maven goals to allow maven users to compile, optimize and test Flex SWF, Flex SWC, Air SWF and Air SWC.
 * Copyright (C) 2008-2012  Marvin Froeder <marvin@flexmojos.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sonatype.flexmojos.compiler;

import static org.sonatype.flexmojos.common.FlexExtension.SWF;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.flexmojos.common.AbstractMavenFlexCompilerConfiguration;
import org.sonatype.flexmojos.common.converter.Module;
import org.sonatype.flexmojos.compiler.command.Result;
import org.sonatype.flexmojos.test.util.PathUtil;
import org.sonatype.flexmojos.truster.FlashPlayerTruster;
import org.sonatype.flexmojos.utilities.SourceFileResolver;

/**
 * <p>
 * Goal which compiles the Flex sources into an application for either Flex or AIR depending on the package type.
 * </p>
 * <p>
 * The Flex Compiler plugin compiles all ActionScript sources. It can compile the source into 'swf' files. The plugin
 * supports 'swf' packaging.
 * </p>
 * 
 * @author Marvin Herman Froeder (velo.br@gmail.com)
 * @since 4.0
 * @goal compile-swf
 * @requiresDependencyResolution compile
 * @phase compile
 * @configurator flexmojos
 */
public class MxmlcMojo
    extends AbstractMavenFlexCompilerConfiguration<MxmlcConfigurationHolder, MxmlcMojo>
    implements ICommandLineConfiguration, Mojo
{

    /**
     * DOCME Again, undocumented by adobe
     * <p>
     * Equivalent to -file-specs
     * </p>
     * Usage:
     * 
     * <pre>
     * &lt;fileSpecs&gt;
     *   &lt;fileSpec&gt;???&lt;/fileSpec&gt;
     *   &lt;fileSpec&gt;???&lt;/fileSpec&gt;
     * &lt;/fileSpecs&gt;
     * </pre>
     * 
     * @parameter
     */
    private List<String> fileSpecs;

    /**
     * A list of resource bundles to include in the output SWC
     * <p>
     * Equivalent to -include-resource-bundles
     * </p>
     * Usage:
     * 
     * <pre>
     * &lt;includeResourceBundles&gt;
     *   &lt;rb&gt;SharedResources&lt;/rb&gt;
     *   &lt;rb&gt;Collections&lt;/rb&gt;
     * &lt;/includeResourceBundles&gt;
     * </pre>
     * 
     * @parameter
     */
    private List<String> includeResourceBundles;

    /**
     * DOCME Another, undocumented by adobe
     * <p>
     * Equivalent to -projector
     * </p>
     * 
     * @parameter expression="${flex.projector}"
     */
    private String projector;

    /**
     * The file to be compiled. The path must be relative with source folder
     * 
     * @parameter expression="${flex.sourceFile}"
     */
    private String sourceFile;

    /**
     * @parameter
     */
    private Module[] modules;

    /**
     * @component
     * @required
     * @readonly
     */
    private FlashPlayerTruster truster;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !PathUtil.exist( getSourcePath() ) )
        {
            getLog().warn( "Skipping compiler, source path doesn't exist." );
            return;
        }

        executeCompiler( new MxmlcConfigurationHolder( this, getSourceFile() ), true );

        if ( runtimeLocales != null )
        {
            List<Result> results = new ArrayList<Result>();
            for ( String locale : runtimeLocales )
            {
                MxmlcMojo cfg = this.clone();
                cfg.compilerLocales = new String[] { locale };
                cfg.classifier = locale;
                results.add( executeCompiler( new MxmlcConfigurationHolder( cfg, null ), fullSynchronization ) );
            }

            wait( results );
        }

        executeModules();
    }

    private void executeModules()
        throws MojoExecutionException, MojoFailureException
    {
        if ( modules != null )
        {
            List<Result> results = new ArrayList<Result>();

            for ( Module module : modules )
            {
                File moduleSource =
                    SourceFileResolver.resolveSourceFile( project.getCompileSourceRoots(), module.getSourceFile() );

                String classifier = FilenameUtils.getBaseName( moduleSource.getName() ).toLowerCase();

                String moduleFinalName;
                if ( module.getFinalName() != null )
                {
                    moduleFinalName = module.getFinalName();
                }
                else
                {
                    moduleFinalName = project.getBuild().getFinalName() + "-" + classifier;
                }

                File moduleOutputDir;
                if ( module.getDestinationPath() != null )
                {
                    moduleOutputDir = new File( project.getBuild().getDirectory(), module.getDestinationPath() );
                }
                else
                {
                    moduleOutputDir = new File( project.getBuild().getDirectory() );
                }

                // TODO include the original extern
                String[] loadExterns = new String[] { getLinkReport() };

                MxmlcMojo cfg = this.clone();
                cfg.classifier = classifier;
                cfg.targetDirectory = moduleOutputDir;
                cfg.finalName = moduleFinalName;
                if ( module.isOptimize() )
                {
                    cfg.loadExterns = PathUtil.getFiles( loadExterns );
                }
                results.add( executeCompiler( new MxmlcConfigurationHolder( cfg, moduleSource ), fullSynchronization ) );
            }

            wait( results );
        }
    }

    public final Result doCompile( MxmlcConfigurationHolder cfg, boolean synchronize )
        throws Exception
    {
        try
        {
            return compiler.compileSwf( cfg, synchronize );
        }
        finally
        {
            truster.updateSecuritySandbox( PathUtil.getCanonicalFile( cfg.getConfiguration().getOutput() ) );
        }

    }

    public List<String> getFileSpecs()
    {
        return fileSpecs;
    }

    public List<String> getIncludeResourceBundles()
    {
        return includeResourceBundles;
    }

    public String getProjector()
    {
        return projector;
    }

    protected File getSourceFile()
    {
        return SourceFileResolver.resolveSourceFile( project.getCompileSourceRoots(), sourceFile, project.getGroupId(),
                                                     project.getArtifactId() );
    }

    @Override
    public final String getProjectType()
    {
        return SWF;
    }

}
