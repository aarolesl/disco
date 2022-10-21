/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.multipreprocessor;

import org.junit.Before;
import org.junit.Test;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ConfigPartitionerTest {
    private PreprocessConfig config;
    private static final Map<String, Set<String>> sourcePaths = new LinkedHashMap<String, Set<String>>() {{
        put("lib1", new LinkedHashSet<>(Arrays.asList("/d1", "/d2", "/d3", "/d4", "/d5", "/d6", "/d7", "/d8")));
        put("lib2", new LinkedHashSet<>(Arrays.asList("/d11", "/d22")));
        put("lib3", new LinkedHashSet<>(Collections.singletonList("/d111")));
    }};
    private static final String outputDir = "/outputDir";
    private static final String agentPath = "/agentpath";
    private static final String jdkPath = "/jdkPath";
    private static final String agentArg = "arg";
    private static final int partitionNum = 3;

    @Before
    public void before() {
        config = PreprocessConfig.builder()
                .sourcePaths(sourcePaths)
                .outputDir(outputDir)
                .agentPath(agentPath)
                .agentArg(agentArg)
                .build();
    }

    @Test
    public void testPartitionConfigGeneratesCorrectNumberOfConfigs_WhenSourcesNoLessThanPartitionNum() {
        List<PreprocessConfig> preprocessorConfigs = ConfigPartitioner.partitionConfig(config, partitionNum);
        assertEquals(partitionNum, preprocessorConfigs.size());
    }

    @Test
    public void testPartitionConfigGeneratesCorrectNumberOfConfigs_WhenSourcesLessThanPartitionNum(){
        Map<String, Set<String>> smallSourcePaths = new LinkedHashMap<String, Set<String>>() {{
            put("lib1", new LinkedHashSet<>(Arrays.asList("/d1", "/d2")));
            put("lib2", new LinkedHashSet<>(Arrays.asList("/d11", "/d22")));
            put("lib3", new LinkedHashSet<>(Collections.singletonList("/d111")));
        }};
        PreprocessConfig configWithSmallSourcePaths = PreprocessConfig.builder()
                .sourcePaths(smallSourcePaths)
                .outputDir(outputDir)
                .agentPath(agentPath)
                .agentArg(agentArg)
                .build();
        assertEquals(2, ConfigPartitioner.partitionConfig(configWithSmallSourcePaths, partitionNum).size());
    }

    @Test
    public void testPartitionConfig_GeneratesConfigsWithCorrectSourcePathsPartition() {
        List<PreprocessConfig> preprocessorConfigs = ConfigPartitioner.partitionConfig(config, partitionNum);
        assertEquals(partitionNum, preprocessorConfigs.size());
        //each preprocessor config should get correct source paths partition
        Map<String, Set<String>> sourcePathsA = preprocessorConfigs.get(0).getSourcePaths();
        Map<String, Set<String>> sourcePathsB = preprocessorConfigs.get(1).getSourcePaths();
        Map<String, Set<String>> sourcePathsC = preprocessorConfigs.get(2).getSourcePaths();
        assertEquals(new LinkedHashSet<>(Arrays.asList("/d1", "/d2", "/d3")), sourcePathsA.get("lib1"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("/d4", "/d5", "/d6")), sourcePathsB.get("lib1"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("/d7", "/d8")), sourcePathsC.get("lib1"));
        assertEquals(new LinkedHashSet<>(Collections.singletonList("/d11")), sourcePathsA.get("lib2"));
        assertEquals(new LinkedHashSet<>(Collections.singletonList("/d22")), sourcePathsB.get("lib2"));
        assertNull(sourcePathsC.get("lib2"));
        assertEquals(new LinkedHashSet<>(Collections.singletonList("/d111")), sourcePathsA.get("lib3"));
        assertNull(sourcePathsB.get("lib3"));
        assertNull(sourcePathsC.get("lib3"));

        //preprocessor configs' source paths partition together should make up source paths of the original config
        Map<String, Set<String>> sourcePaths = config.getSourcePaths();
        List<Map<String, Set<String>>> preprocessorSourcePaths = Arrays.asList(sourcePathsA, sourcePathsB, sourcePathsC);
        for(String key : sourcePaths.keySet()) {
            Set<String> mergedSources = new HashSet<>();
            for(Map<String, Set<String>> sourcePathsMap : preprocessorSourcePaths) {
                if(sourcePathsMap.containsKey(key)){
                    mergedSources.addAll(sourcePathsMap.get(key));
                }
            }
            assertEquals(sourcePaths.get(key), mergedSources);
        }
    }

    @Test
    public void testPartitionConfigGeneratesConfigsWithSameValuesAsOriginalConfigExceptSourcePathsAndJdkPath() {
        List<PreprocessConfig> preprocessorConfigs = ConfigPartitioner.partitionConfig(config, partitionNum);
        for (PreprocessConfig preprocessorConfig : preprocessorConfigs) {
            //for fields that already been populated in original config, should have same values in preprocessor config
            assertEquals(config.getOutputDir(), preprocessorConfig.getOutputDir());
            assertEquals(config.getAgentPath(), preprocessorConfig.getAgentPath());
            assertEquals(config.getAgentArg(), preprocessorConfig.getAgentArg());
            assertEquals(config.getSignedJarHandlingStrategy().getClass(), preprocessorConfig.getSignedJarHandlingStrategy().getClass());
            assertEquals(config.getLogLevel(), preprocessorConfig.getLogLevel());
            assertEquals(config.isFailOnUnresolvableDependency(), preprocessorConfig.isFailOnUnresolvableDependency());
            //for those fields that are not populated in preprocessing config, should either not be set in preprocessor config
            assertNull(preprocessorConfig.getSuffix());
            assertNull(preprocessorConfig.getSerializationJarPath());
            assertNull(preprocessorConfig.getJavaVersion());
            assertNull(preprocessorConfig.getJdkPath());
        }
    }

    @Test
    public void testPartitionConfigGeneratesConfigsWithNullJdkPath_WhenJdkPathIsNull() {
        List<PreprocessConfig> preprocessorConfigs = ConfigPartitioner.partitionConfig(config, partitionNum);
        assertNull(config.getJdkPath());
        //all preprocessors configs should have null jdkPath
        assertNull(preprocessorConfigs.get(0).getJdkPath());
        assertNull(preprocessorConfigs.get(1).getJdkPath());
        assertNull(preprocessorConfigs.get(2).getJdkPath());
    }

    @Test
    public void testPartitionConfigGeneratesOnlyFirstConfigWithJdkPath_WhenJdkPathNotNull() {
        PreprocessConfig config = PreprocessConfig.builder()
                .sourcePaths(sourcePaths)
                .outputDir(outputDir)
                .agentPath(agentPath)
                .agentArg(agentArg)
                .jdkPath(jdkPath)
                .build();
        List<PreprocessConfig> preprocessorConfigs = ConfigPartitioner.partitionConfig(config, partitionNum);

        assertNotNull(config.getJdkPath());
        //only first preprocessor config will have jdkPath set
        assertEquals(config.getJdkPath(), preprocessorConfigs.get(0).getJdkPath());
        //other preprocessor configs will have null jdkPath
        assertNull(preprocessorConfigs.get(1).getJdkPath());
        assertNull(preprocessorConfigs.get(2).getJdkPath());
    }
}
