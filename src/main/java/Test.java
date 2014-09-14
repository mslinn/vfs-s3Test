import com.intridea.io.vfs.provider.s3.S3FileProvider;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

import java.io.FileInputStream;
import java.util.Properties;

public class Test {
    public static void main(String[] args) throws Exception {
        Properties config = new Properties();
        config.load(new FileInputStream(System.getProperty("user.home") + "/.aws/config")); // same authentication file used by aws-cli
        StaticUserAuthenticator auth = new StaticUserAuthenticator(null, config.getProperty("aws_access_key_id"), config.getProperty("aws_secret_access_key"));
        FileSystemOptions opts = S3FileProvider.getDefaultFileSystemOptions();
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);

        FileObject[] files = VFS.getManager().resolveFile("s3://mslinntest/testDir").getChildren();
        for (FileObject file : files)
          System.out.println(file);
    }
}
