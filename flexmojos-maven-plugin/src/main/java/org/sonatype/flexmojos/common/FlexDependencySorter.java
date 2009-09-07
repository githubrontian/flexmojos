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
package org.sonatype.flexmojos.common;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonatype.flexmojos.utilities.MavenUtils;

/**
 * @since 3.4
 */
public class FlexDependencySorter
{
    private static final String DEPENDENCY_TRAIL_SWC = ":" + FlexExtension.SWC + ":";

    private static final String[] FDK_VERSION_ARTIFACTS_IDS =
        new String[] { "flex-framework", "air-framework", "framework", "airframework" };

    private static final String PLAYER_GLOBAL = "playerglobal";

    private static final String AIR_GLOBAL = "airglobal";

    private MavenProject project;

    private String fdkVersion;

    private File fdkConfigFile;

    private boolean isAIR;

    private List<File> linkReports = new ArrayList<File>();

    protected List<File> externalLibraries = new ArrayList<File>();

    protected List<File> internalLibraries = new ArrayList<File>();

    protected List<File> mergedLibraries = new ArrayList<File>();

    protected List<File> testLibraries = new ArrayList<File>();

    private List<Artifact> resourceBundleArtifacts = new ArrayList<Artifact>();

    private List<File> globalLibraries = new ArrayList<File>();

    private Artifact globalArtifact;

    public File getFDKConfigFile()
    {
        return fdkConfigFile;
    }

    public String getFDKVersion()
    {
        return fdkVersion;
    }

    public boolean isAIR()
    {
        return isAIR;
    }

    public Artifact getGlobalArtifact()
    {
        return globalArtifact;
    }

    public File[] getGlobalLibraries()
    {
        return toArray( globalLibraries );
    }

    public File[] getLinkReports()
    {
        return toArray( linkReports );
    }

    public File[] getExternalLibraries()
    {
        return toArray( externalLibraries );
    }

    public File[] getInternalLibraries()
    {
        return toArray( internalLibraries );
    }

    public File[] getMergedLibraries()
    {
        return toArray( mergedLibraries );
    }

    public File[] getTestLibraries()
    {
        return toArray( testLibraries );
    }

    public List<Artifact> getResourceBundleArtifacts()
    {
        return resourceBundleArtifacts;
    }

    @SuppressWarnings( "unchecked" )
    public void sort( MavenProject project )
        throws MojoExecutionException
    {
        this.project = project;

        for ( Artifact artifact : (Set<Artifact>) project.getArtifacts() )
        {
            sortArtifact( artifact );
        }

        if ( globalArtifact == null )
        {
            throw new MojoExecutionException( "Player/Air Global dependency not found." );
        }
        else
        {
            isAIR = AIR_GLOBAL.equals( globalArtifact.getArtifactId() );
        }
    }

    /**
     * @return true if artifact sorted, false otherwise
     */
    protected boolean sortArtifact( Artifact artifact )
        throws MojoExecutionException
    {
        if ( FlexClassifier.LINK_REPORT.equals( artifact.getClassifier() ) )
        {
            linkReports.add( artifact.getFile() );
        }
        else if ( FlexExtension.SWC.equals( artifact.getType() ) )
        {
            if ( PLAYER_GLOBAL.equals( artifact.getArtifactId() ) || AIR_GLOBAL.equals( artifact.getArtifactId() ) )
            {
                sortGlobalArtifact( artifact );
            }
            else
            {
                return sortSWCArtifact( artifact );
            }
        }
        else if ( FlexExtension.RB_SWC.equals( artifact.getType() ) )
        {
            resourceBundleArtifacts.add( artifact );
        }
        else if ( fdkConfigFile == null && artifact.getGroupId().equals( "com.adobe.flex.framework" ) )
        {
            checkFDKConfigAndVersion( artifact );
        }
        else
        {
            return false;
        }

        return true;
    }

    protected boolean sortSWCArtifact( Artifact artifact )
        throws MojoExecutionException
    {
        final String scope = artifact.getScope();
        if ( scope.equals( Artifact.SCOPE_COMPILE ) )
        {
            addToDefaultScope( artifact );
        }
        else if ( scope.equals( FlexScopes.EXTERNAL ) )
        {
            externalLibraries.add( artifact.getFile() );
        }
        else if ( scope.equals( FlexScopes.INTERNAL ) )
        {
            internalLibraries.add( artifact.getFile() );
        }
        else if ( scope.equals( FlexScopes.MERGED ) )
        {
            mergedLibraries.add( artifact.getFile() );
        }
        else if ( scope.equals( Artifact.SCOPE_TEST ) )
        {
            testLibraries.add( artifact.getFile() );
        }
        else
        {
            return false;
        }

        return true;
    }

    protected void addToDefaultScope( Artifact artifact )
        throws MojoExecutionException
    {
        externalLibraries.add( artifact.getFile() );
    }

    private void sortGlobalArtifact( Artifact artifact )
        throws MojoExecutionException
    {
        List<String> dependencyTrail = artifact.getDependencyTrail();
        // i = 1, because first item is project artifact; size - 1, because last item is current artifact
        for ( int i = 1, n = dependencyTrail.size() - 1; i < n; i++ )
        {
            if ( dependencyTrail.get( i ).lastIndexOf( DEPENDENCY_TRAIL_SWC ) != -1 )
            {
                return;
            }
        }

        if ( globalArtifact != null )
        {
            throw new MojoExecutionException( "Player/Air Global dependency already specified.\n" + "First:  "
                + globalArtifact + "\nSecond: " + artifact );
        }

        globalArtifact = artifact;
        globalLibraries.add( MavenUtils.getArtifactFile( artifact, project.getBuild() ) );
    }

    private void checkFDKConfigAndVersion( Artifact artifact )
    {
        if ( FlexClassifier.CONFIGS.equals( artifact.getClassifier() ) )
        {
            fdkConfigFile = artifact.getFile();
            if ( fdkVersion == null )
            {
                fdkVersion = artifact.getVersion();
            }
        }
        else if ( fdkVersion == null && MavenExtension.POM.equals( artifact.getType() ) )
        {
            final String artifactId = artifact.getArtifactId();
            for ( String fdkId : FDK_VERSION_ARTIFACTS_IDS )
            {
                if ( artifactId.equals( fdkId ) )
                {
                    fdkVersion = artifact.getVersion();
                }
            }
        }
    }

    /**
     * Vladimir Krivosheev: temp, we remove it after "flexmojos to stop using OEM API and use a lower level API"
     */
    private Map<List<File>, File[]> listArrayMap = new HashMap<List<File>, File[]>( 8 );

    protected File[] toArray( List<File> list )
    {
        if ( listArrayMap.containsKey( list ) )
        {
            return listArrayMap.get( list );
        }
        else
        {
            File[] array = list.toArray( new File[list.size()] );
            listArrayMap.put( list, array );
            return array;
        }
    }
}
