/**
 *  Copyright 2008 Marvin Herman Froeder
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */
package org.sonatype.flexmojos.compiler;

import static org.sonatype.flexmojos.common.FlexExtension.SWC;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Build a SWC of the test classes for the current project.
 * 
 * @author Marvin Herman Froeder (velo.br@gmail.com)
 * @since 2.0
 * @goal test-swc
 * @requiresDependencyResolution
 */
public class TestLibraryCompilerMojo
    extends LibraryMojo
{

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File testFolder = new File( build.getTestSourceDirectory() );

        if ( testFolder.exists() )
        {
            setUp();
            run();
            tearDown();
        }
        else
        {
            getLog().warn( "Test folder not found." );
        }

    }

    @Override
    public void setUp()
        throws MojoExecutionException, MojoFailureException
    {
        File outputFolder = new File( build.getTestOutputDirectory() );
        if ( !outputFolder.exists() )
        {
            outputFolder.mkdirs();
        }

        includeSources = getValidSourceRoots( project.getTestCompileSourceRoots() ).toArray( new File[0] );

        super.setUp();
    }

    @Override
    protected void configure()
        throws MojoExecutionException, MojoFailureException
    {
        super.configure();

        // add test libraries
        configuration.addLibraryPath( getDependenciesPath( "test" ) );

        configuration.addSourcePath( getValidSourceRoots( project.getTestCompileSourceRoots() ).toArray( new File[0] ) );
    }

    @Override
    protected void attachArtifact()
    {
        projectHelper.attachArtifact( project, SWC, "test", getOutput() );
    }

    @Override
    protected File getOutput()
    {
        return new File( build.getDirectory(), build.getFinalName() + "-test.swc" );
    }

}
