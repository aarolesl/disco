/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.instrumentation.preprocess.cli;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.disco.agent.logging.Logger;
import software.amazon.disco.instrumentation.preprocess.TestUtils;
import software.amazon.disco.instrumentation.preprocess.exceptions.ArgumentParserException;
import software.amazon.disco.instrumentation.preprocess.exceptions.InvalidConfigEntryException;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.SkipSignedJarHandlingStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.cache.ChecksumCacheStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.cache.NoOpCacheStrategy;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreprocessConfigParserTest {
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();
    static String jdkpath;

    String outputDir = "/d";
    String agent = "/agent_path";
    String suffix = "-suffix";

    static PreprocessConfigParser preprocessConfigParser;

    @BeforeClass
    public static void beforeAll() throws IOException {
        jdkpath = tempFolder.newFolder("java").getAbsolutePath();
        File fakeJDK9ModuleFile = new File(jdkpath, "jmods/java.base.jmod");
        fakeJDK9ModuleFile.mkdirs();
    }

    @Before
    public void before() {
        preprocessConfigParser = new PreprocessConfigParser();
    }

    @Test(expected = ArgumentParserException.class)
    public void testParseCommandLineReturnsNullWithNullArgs() {
        preprocessConfigParser.parseCommandLine(null);
    }

    @Test(expected = ArgumentParserException.class)
    public void testParseCommandLineFailsWithEmptyArgs() {
        preprocessConfigParser.parseCommandLine(new String[]{});
    }

    @Test(expected = ArgumentParserException.class)
    public void testParseCommandLineFailsWithInvalidFlag() {
        String[] args = new String[]{"--suff", suffix};
        preprocessConfigParser.parseCommandLine(args);
    }

    @Test(expected = ArgumentParserException.class)
    public void testParseCommandLineFailsWithUnmatchedFlagAsLastArg() {
        String[] args = new String[]{"--suffix"};
        preprocessConfigParser.parseCommandLine(args);
    }

    @Test(expected = ArgumentParserException.class)
    public void testParseCommandLineFailsWithInvalidFormat() {
        String[] args = new String[]{"--suffix", "--verbose"};
        preprocessConfigParser.parseCommandLine(args);
    }

    @Test
    public void testParseCommandLineWorksWithDifferentLogLevels() {
        PreprocessConfig silentConfig = preprocessConfigParser.parseCommandLine(new String[]{"--silent"});
        PreprocessConfig verboseConfig = preprocessConfigParser.parseCommandLine(new String[]{"--verbose"});
        PreprocessConfig extraverboseConfig = preprocessConfigParser.parseCommandLine(new String[]{"--extraverbose"});

        assertEquals(Logger.Level.FATAL, silentConfig.getLogLevel());
        assertEquals(Logger.Level.DEBUG, verboseConfig.getLogLevel());
        assertEquals(Logger.Level.TRACE, extraverboseConfig.getLogLevel());
    }

    @Test
    public void testParseCommandLineWorksWhenCacheStrategyIsNone() {
        String[] args = new String[]{
            "--sourcepaths", "/d1:/d2:/d3",
            "--agentPath", agent,
            "--cacheStrategy", "none"
        };
        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertTrue(config.getCacheStrategy() instanceof NoOpCacheStrategy);
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testParseCommandLineFailsWithNonNumberFormatWorkersValue() {
        String[] args = new String[]{"--workers", "NonNumberFormat"};
        preprocessConfigParser.parseCommandLine(args);
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testParseCommandLineFailsWithNonPositiveWorkersValue() {
        String[] args = new String[]{"--workers", "-1"};
        preprocessConfigParser.parseCommandLine(args);
    }

    @Test
    public void testParseCommandLineWorksAndReturnsConfigWithDefaultValue() {
        String[] args = new String[]{
            "--sourcepaths", "/d1:/d2:/d3",
            "--agentPath", agent
        };
        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertFalse(config.isFailOnUnresolvableDependency());
        assertEquals(Logger.Level.INFO, config.getLogLevel());
        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/d3")), config.getSourcePaths().get(""));
        assertTrue(config.getSignedJarHandlingStrategy() instanceof InstrumentSignedJarHandlingStrategy);
        assertTrue(config.getCacheStrategy() instanceof NoOpCacheStrategy);
    }

    @Test
    public void testParseCommandLineWorksWithFullCommandNamesAndReturnsConfigFile() {
        String[] args = new String[]{
            "--outputDir", outputDir,
            "--sourcepaths", "/d1:/d2:/d3@lib",
            "--agentPath", agent,
            "--suffix", suffix,
            "--javaversion", "11",
            "--agentarg", "arg",
            "--jdksupport", jdkpath,
            "--failonunresolvabledependency",
            "--signedjarhandlingstrategy", "skip",
            "--cachestrategy", "checksum",
            "--workers", "3"
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(outputDir, config.getOutputDir());
        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/d3")), config.getSourcePaths().get("lib"));
        assertEquals(agent, config.getAgentPath());
        assertEquals(suffix, config.getSuffix());
        assertEquals("11", config.getJavaVersion());
        assertEquals("arg", config.getAgentArg());
        assertEquals(jdkpath, config.getJdkPath());
        assertEquals("3", config.getSubPreprocessors());
        assertTrue(config.isFailOnUnresolvableDependency());
        assertTrue(config.getSignedJarHandlingStrategy() instanceof SkipSignedJarHandlingStrategy);
        assertTrue(config.getCacheStrategy() instanceof ChecksumCacheStrategy);
    }

    @Test
    public void testParseCommandLineWorksWithShortHandCommandNamesAndReturnsConfigFile() {
        String[] args = new String[]{
            "-out", outputDir,
            "-sps", "/d1:/d2:/D3@lib",
            "-ap", agent,
            "-suf", suffix,
            "-jv", "11",
            "-arg", "arg",
            "-jdks", jdkpath,
            "-cache", "checksum"
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(outputDir, config.getOutputDir());
        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/D3")), config.getSourcePaths().get("lib"));
        assertEquals(agent, config.getAgentPath());
        assertEquals(suffix, config.getSuffix());
        assertEquals("11", config.getJavaVersion());
        assertEquals("arg", config.getAgentArg());
        assertEquals(jdkpath, config.getJdkPath());
        assertTrue(config.getCacheStrategy() instanceof ChecksumCacheStrategy);
    }

    @Test
    public void testParseCommandLineWorksWithPathToResponseFile() throws Exception {
        String fileContent = " -out " + outputDir + " -ap " + agent;
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        String[] args = new String[]{
            "@" + responseFile.getAbsolutePath()
        };

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);
        assertEquals(outputDir, config.getOutputDir());
        assertEquals(agent, config.getAgentPath());
    }

    @Test
    public void testParseCommandLineAppendsResponseFileArgsWithCommandLineArgs() throws Exception {
        String fileContent = " -out " + outputDir + " -ap " + agent;
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        String[] args = new String[]{"@" + responseFile.getAbsolutePath(), "-sps", "/d1:/d1:/d2", "-out", "new_value"};

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(agent, config.getAgentPath());
        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2")), config.getSourcePaths().get(""));

        // value from response file should be overridden by value from command line arg
        assertEquals("new_value", config.getOutputDir());
    }

    @Test
    public void testParseCommandLineJoinsResponseFileSourcePathsWithCommandLineSourcePaths() throws Exception {
        String fileContent = "-sps /d3:/d4:/d5 -sps /d6@lib";
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        String[] args = new String[]{"@" + responseFile.getAbsolutePath(), "-sps", "/d1:/d1:/d2"};

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(new HashSet<>(Arrays.asList("/d1", "/d2", "/d3", "/d4", "/d5")), config.getSourcePaths().get(""));
        assertEquals(new HashSet<>(Arrays.asList("/d6")), config.getSourcePaths().get("lib"));
    }

    @Test
    public void testParseCommandLineWorksWithDuplicatePaths() {
        String[] args = new String[]{"-sps", "/d1:/d1:/d2@lib"};

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(1, config.getSourcePaths().size());
        assertEquals(2, config.getSourcePaths().get("lib").size());
    }

    @Test(expected = InvalidConfigEntryException.class)
    public void testParseCommandLineFailsWithInvalidSourcePathOption() {
        String[] args = new String[]{"-sps", "/d1:/d1:/d2@lib@tomcat",};

        preprocessConfigParser.parseCommandLine(args);
    }

    @Test
    public void testParseCommandLineWorksWithAllEmptyPaths() {
        String[] args = new String[]{"-sps", ":@lib"};

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertFalse(config.getSourcePaths().containsKey("lib"));
    }

    @Test
    public void testParseCommandLineWorksAndFilteredEmptyPaths() {
        String[] args = new String[]{"-sps", "::/d1@lib"};

        PreprocessConfig config = preprocessConfigParser.parseCommandLine(args);

        assertEquals(1, config.getSourcePaths().get("lib").size());
        assertEquals(new HashSet<>(Arrays.asList("/d1")), config.getSourcePaths().get("lib"));
    }

    @Test
    public void testReadOptionsFromFileWorksWithSingleWhiteSpace() throws Exception {
        String fileContent = " -out " + outputDir + " -ap " + agent;
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        List<String> args = preprocessConfigParser.readArgsFromFile(responseFile.getAbsolutePath());

        assertEquals(4, args.size());
        assertEquals("-out", args.get(0));
        assertEquals(outputDir, args.get(1));
        assertEquals("-ap", args.get(2));
        assertEquals(agent, args.get(3));
    }

    @Test
    public void testReadOptionsFromFileWorksWithMultipleWhiteSpaceAndTab() throws Exception {
        String fileContent = "   -out   " + outputDir + " -ap\t" + agent;
        File responseFile = TestUtils.createFile(tempFolder.getRoot(), "@response.txt", fileContent.getBytes());

        List<String> args = preprocessConfigParser.readArgsFromFile(responseFile.getAbsolutePath());

        assertEquals(4, args.size());
        assertEquals("-out", args.get(0));
        assertEquals(outputDir, args.get(1));
        assertEquals("-ap", args.get(2));
        assertEquals(agent, args.get(3));
    }

    @Test(expected = ArgumentParserException.class)
    public void testReadOptionsFromFileFailsWhenPathToResponseFileIsNonExistent() {
        preprocessConfigParser.readArgsFromFile("path_to_file");
    }

    @Test(expected = ArgumentParserException.class)
    public void testReadOptionsFromFileFailsWhenPathToResponseFileIsADirectory() {
        preprocessConfigParser.readArgsFromFile(tempFolder.getRoot().getAbsolutePath());
    }
}
