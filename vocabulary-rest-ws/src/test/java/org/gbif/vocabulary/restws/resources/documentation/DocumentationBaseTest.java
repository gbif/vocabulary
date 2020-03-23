package org.gbif.vocabulary.restws.resources.documentation;

import org.gbif.api.vocabulary.TranslationLanguage;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;
import org.gbif.vocabulary.model.search.KeyNameResult;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import capital.scalable.restdocs.AutoDocumentation;
import capital.scalable.restdocs.jackson.JacksonResultHandlers;
import capital.scalable.restdocs.response.ResponseModifyingPreprocessors;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.cli.CliDocumentation;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.http.HttpDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Base64Utils;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base class for the tests that generate documentation. */
@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.liquibase.enabled=false"})
@ActiveProfiles({"mock", "test"})
abstract class DocumentationBaseTest {

  static final Long TEST_KEY = 1L;
  static final String TEST_VOCABULARY_NAME = "vocab1";
  static final Long TEST_VOCABULARY_KEY = 1L;
  static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static final Supplier<String> BASIC_AUTH_HEADER =
      () ->
          "Basic " + Base64Utils.encodeToString("example:example".getBytes(StandardCharsets.UTF_8));

  @Autowired private Filter springSecurityFilterChain;
  @Autowired private WebApplicationContext context;

  MockMvc mockMvc;

  @BeforeEach
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(this.context)
            .alwaysDo(JacksonResultHandlers.prepareJackson(OBJECT_MAPPER))
            .alwaysDo(
                MockMvcRestDocumentation.document(
                    "{class-name}/{method-name}",
                    preprocessRequest(),
                    Preprocessors.preprocessResponse(
                        ResponseModifyingPreprocessors.replaceBinaryContent(),
                        ResponseModifyingPreprocessors.limitJsonArrayLength(OBJECT_MAPPER),
                        Preprocessors.prettyPrint())))
            .apply(
                MockMvcRestDocumentation.documentationConfiguration(restDocumentation)
                    .uris()
                    .withScheme("http")
                    .withHost("api.gbif-dev.org")
                    .withPort(80)
                    .and()
                    .snippets()
                    .withDefaults(
                        CliDocumentation.curlRequest(),
                        HttpDocumentation.httpRequest(),
                        HttpDocumentation.httpResponse(),
                        AutoDocumentation.pathParameters(),
                        AutoDocumentation.requestParameters(),
                        AutoDocumentation.description(),
                        AutoDocumentation.methodAndPath(),
                        AutoDocumentation.section()))
            .build();
  }

  static void setSecurityContext() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", "admin", Collections.singletonList(new SimpleGrantedAuthority("admin"))));
  }

  RequestPostProcessor authorizationDocumentation() {
    return request -> {
      request.addHeader(
          "Authorization",
          "Basic "
              + java.util.Base64.getEncoder()
                  .encodeToString(String.format("%s:%s", "user", "password").getBytes()));
      return request;
    };
  }

  Vocabulary createVocabulary(String name) {
    Vocabulary vocabulary = new Vocabulary();
    vocabulary.setName(name);
    vocabulary.setLabel(Collections.singletonMap(TranslationLanguage.ENGLISH, "Label"));
    vocabulary.setNamespace("ns");
    vocabulary.setEditorialNotes(Arrays.asList("note1", "note2"));

    return vocabulary;
  }

  Concept createConcept(String name) {
    Concept concept = new Concept();
    concept.setVocabularyKey(TEST_VOCABULARY_KEY);
    concept.setName(name);
    concept.setLabel(Collections.singletonMap(TranslationLanguage.ENGLISH, "Label"));
    concept.setAlternativeLabels(
        Collections.singletonMap(
            TranslationLanguage.ENGLISH, Arrays.asList("Alt label", "Another alt label")));
    concept.setMisappliedLabels(
        Collections.singletonMap(TranslationLanguage.ENGLISH, Collections.singletonList("Labl")));
    concept.setEditorialNotes(Arrays.asList("note1", "note2"));

    return concept;
  }

  void suggestTest(List<KeyNameResult> suggestions) throws Exception {
    MvcResult mvcResult =
        mockMvc
            .perform(get(getBasePath() + "/suggest?q=foo"))
            .andExpect(status().isOk())
            .andReturn();

    List<KeyNameResult> resultList =
        OBJECT_MAPPER.readValue(
            mvcResult.getResponse().getContentAsString(),
            new TypeReference<List<KeyNameResult>>() {});
    assertEquals(suggestions.size(), resultList.size());
  }

  List<KeyNameResult> createSuggestions() {
    KeyNameResult keyNameResult1 = new KeyNameResult();
    keyNameResult1.setKey(1);
    keyNameResult1.setName("foo1");
    KeyNameResult keyNameResult2 = new KeyNameResult();
    keyNameResult2.setKey(2);
    keyNameResult2.setName("foo2");
    return ImmutableList.of(keyNameResult1, keyNameResult2);
  }

  ResultHandler documentFields(Class clazz) {
    return document(
        "{class-name}/{method-name}",
        PayloadDocumentation.relaxedRequestFields(getFields(clazz)),
        PayloadDocumentation.relaxedResponseFields(getFields(clazz)));
  }

  ResultHandler documentRequestFields(Class clazz) {
    return document(
        "{class-name}/{method-name}", PayloadDocumentation.relaxedRequestFields(getFields(clazz)));
  }

  List<FieldDescriptor> getFields(Class clazz) {
    List<Field> fields = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
    fields.addAll(Arrays.asList(clazz.getSuperclass().getDeclaredFields()));

    ConstraintDescriptions fieldConstraints = new ConstraintDescriptions(clazz);

    return fields.stream()
        .filter(f -> !f.isSynthetic())
        .map(
            f -> {
              FieldDescriptor fd =
                  PayloadDocumentation.fieldWithPath(f.getName())
                      .description(
                          String.join(",", fieldConstraints.descriptionsForProperty(f.getName())))
                      .type(f.getType().getSimpleName());

              boolean required =
                  f.getAnnotation(NotNull.class) != null || f.getAnnotation(NotBlank.class) != null;
              if (!required) {
                fd.optional();
              }

              return fd;
            })
        .collect(Collectors.toList());
  }

  abstract String getBasePath();
}
