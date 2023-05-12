package com.getyourguide.openapi.validation.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommaSeparatedStringsUtilTest {
    @Test
    void convertCommaSeparatedStringToSet() {
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet(null)).isEmpty();
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet("")).isEmpty();
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet(" ")).isEmpty();
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet(" , ")).isEmpty();

        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet("a")).containsExactlyInAnyOrder("a");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet(" a ")).containsExactlyInAnyOrder("a");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet("a,b")).containsExactlyInAnyOrder("a", "b");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet("a, b")).containsExactlyInAnyOrder("a", "b");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet("a , b")).containsExactlyInAnyOrder("a", "b");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet("a ,b")).containsExactlyInAnyOrder("a", "b");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet("a, b ")).containsExactlyInAnyOrder("a", "b");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet(" a , b ")).containsExactlyInAnyOrder("a", "b");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet(" a , b ")).containsExactlyInAnyOrder("a", "b");
        assertThat(CommaSeparatedStringsUtil.convertCommaSeparatedStringToSet("a, , b")).containsExactlyInAnyOrder("a", "b");
    }
}
