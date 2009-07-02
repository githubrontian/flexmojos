package org.sonatype.flexmojos.test.launcher;

import java.io.File;
import java.net.URISyntaxException;

import org.codehaus.plexus.PlexusTestNGCase;
import org.codehaus.plexus.context.Context;
import org.sonatype.flexmojos.test.util.OSUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class AbstractAsVmLauncherTest
    extends PlexusTestNGCase
{

    protected AsVmLauncher launcher;

    protected static final File VALID_SWF;

    protected static final File INVALID_SWF;

    static
    {
        try
        {
            VALID_SWF = new File( AsVmLauncherTest.class.getResource( "/SelftExit.swf" ).toURI() );
            INVALID_SWF = new File( AsVmLauncherTest.class.getResource( "/NonExit.swf" ).toURI() );
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException();// won't happen, I hope =D
        }
    }

    @BeforeMethod
    public void setUp()
        throws Exception
    {
        launcher = lookup( AsVmLauncher.class );
    }

    @AfterMethod
    public void tearDown()
        throws Exception
    {
        launcher.stop();

        System.out.println( launcher.getConsoleOutput() );
    }

    @Override
    protected void customizeContext( Context context )
    {
        super.customizeContext( context );

        context.put( "flashplayer.command", OSUtils.getPlatformDefaultCommand() );
    }

}