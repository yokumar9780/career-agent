package com.careeragent.service;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **Validates: Requirements 13.4**
 */
@Label("Feature: career-agent, Property 16: Input Sanitization Idempotence")
class SanitizationPropertyTest {

    private final ValidationService validationService = new ValidationService();

    @Property(tries = 200)
    @Label("Sanitization is idempotent — applying twice yields same result as once")
    void sanitizeIsIdempotent(@ForAll("htmlStrings") String input) {
        String once = validationService.sanitizeText(input);
        String twice = validationService.sanitizeText(once);
        assertThat(twice).isEqualTo(once);
    }

    @Property(tries = 200)
    @Label("Script tags are completely removed from output")
    void scriptTagsAreRemoved(@ForAll("stringsWithScriptTags") String input) {
        String result = validationService.sanitizeText(input);
        assertThat(result).doesNotContainIgnoringCase("<script");
        assertThat(result).doesNotContainIgnoringCase("</script>");
    }

    @Property(tries = 200)
    @Label("HTML tags are stripped but text content is preserved")
    void htmlTagsStrippedContentPreserved(@ForAll("stringsWithHtmlTags") String input) {
        String result = validationService.sanitizeText(input);
        assertThat(result).doesNotContain("<").doesNotContain(">");
    }

    @Property(tries = 100)
    @Label("Null input returns null")
    void nullInputReturnsNull() {
        assertThat(validationService.sanitizeText(null)).isNull();
    }

    @Property(tries = 200)
    @Label("Plain text without HTML is preserved after sanitization")
    void plainTextIsPreserved(@ForAll("plainTextStrings") String input) {
        String result = validationService.sanitizeText(input);
        assertThat(result).isEqualTo(input.trim());
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> htmlStrings() {
        Arbitrary<String> plainText = Arbitraries.strings()
                .alpha()
                .ofMinLength(0)
                .ofMaxLength(30);
        Arbitrary<String> htmlTag = Arbitraries.of(
                "<b>bold</b>", "<i>italic</i>", "<div>content</div>",
                "<p>paragraph</p>", "<span class=\"x\">text</span>",
                "<a href=\"url\">link</a>", "<br/>", "<img src=\"x\"/>",
                "<script>alert('xss')</script>", "<SCRIPT>bad()</SCRIPT>",
                "<script type=\"text/javascript\">evil()</script>",
                "nested <b><i>tags</i></b>", "plain text no tags",
                "<div><p>deep<b>nesting</b></p></div>"
        );
        return Combinators.combine(plainText, htmlTag, plainText)
                .as((before, tag, after) -> before + tag + after);
    }

    @Provide
    Arbitrary<String> stringsWithScriptTags() {
        Arbitrary<String> prefix = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(10);
        Arbitrary<String> scriptContent = Arbitraries.of(
                "<script>alert('xss')</script>",
                "<SCRIPT>malicious()</SCRIPT>",
                "<script type=\"text/javascript\">code()</script>",
                "<Script>mixed()</Script>"
        );
        Arbitrary<String> suffix = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(10);
        return Combinators.combine(prefix, scriptContent, suffix)
                .as((p, s, suf) -> p + s + suf);
    }

    @Provide
    Arbitrary<String> stringsWithHtmlTags() {
        Arbitrary<String> prefix = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(10);
        Arbitrary<String> tag = Arbitraries.of(
                "<b>text</b>", "<div>content</div>", "<p>para</p>",
                "<span>inline</span>", "<a href=\"#\">link</a>",
                "<img src=\"x\"/>", "<br/>"
        );
        Arbitrary<String> suffix = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(10);
        return Combinators.combine(prefix, tag, suffix)
                .as((p, t, s) -> p + t + s);
    }

    @Provide
    Arbitrary<String> plainTextStrings() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50);
    }
}
