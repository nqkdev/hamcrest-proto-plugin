package me.nqkdev.plugins.it;

import different.packageName.ImportSampleMatchers;
import different.packageName.ImportSampleOuterClass.ImportSample;
import me.nqkdev.plugins.it.SampleMesage.Sample;
import me.nqkdev.plugins.it.SampleMesage.Sample.SubSample;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import java.util.Arrays;

import static me.nqkdev.plugins.it.SampleMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasValue;
import static org.hamcrest.core.Is.is;

public class MatcherGeneratorTest {

    private static final SubSample SUB_SAMPLE = SubSample.newBuilder().setSubSample("subSample").build();

    private static final ImportSample IMPORT_SAMPLE = ImportSample.newBuilder().setImportSampleInteger(92).build();

    private static final Sample SAMPLE = Sample.newBuilder()
            .setSimpleString("simpleString")
            .setSimpleInteger(29L)
            .addAllRepeatedString(Arrays.asList("string1", "string2"))
            .putMapString("simple", "map")

            .setSubSample(SUB_SAMPLE)
            .addAllRepeatedSubSample(Arrays.asList(SUB_SAMPLE, SUB_SAMPLE, SUB_SAMPLE))
            .putMapSubSample("subSample", SUB_SAMPLE)

            .setImportSample(IMPORT_SAMPLE)

            .build();

    @Test
    public void testSample() {
        assertThat(SAMPLE, withSimpleString(is("simpleString")));
        assertThat(SAMPLE, withSimpleInteger(is(29L)));
        assertThat(SAMPLE, withRepeatedString(hasSize(2)));
        assertThat(SAMPLE, withMapString(hasEntry("simple", "map")));

        assertThat(SAMPLE, withSubSample(
                SubSampleMatchers.withSubSample(is("subSample"))
        ));
        assertThat(SAMPLE, withRepeatedSubSample(
                IsCollectionContaining.hasItem(SubSampleMatchers.withSubSample(is("subSample")))
        ));
        assertThat(SAMPLE, withMapSubSample(hasValue(is(SubSampleMatchers.withSubSample(is("subSample"))))));

        assertThat(SAMPLE, withImportSample(
                ImportSampleMatchers.withImportSampleInteger(is(92))
        ));
    }

}
