package org.gbif.vocabulary.importer.http;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.vocabulary.model.Concept;
import org.gbif.vocabulary.model.Vocabulary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import static org.gbif.vocabulary.importer.http.SyncCall.syncCall;

/** A lightweight Vocabulary client. */
public class VocabularyClient {

  private final API api;

  private VocabularyClient(String url, String user, String password) {
    Objects.requireNonNull(url);

    ObjectMapper mapper =
        new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .registerModule(new JavaTimeModule());

    OkHttpClient.Builder okHttpClientBuilder =
        new OkHttpClient.Builder()
            .cache(null)
            .connectTimeout(Duration.ofMinutes(2))
            .readTimeout(Duration.ofMinutes(2));

    if (user != null && password != null) {
      okHttpClientBuilder.addInterceptor(new BasicAuthInterceptor(user, password)).build();
    }

    Retrofit retrofit =
        new Retrofit.Builder()
            .client(okHttpClientBuilder.build())
            .baseUrl(url)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
    api = retrofit.create(API.class);
  }

  public static VocabularyClient create(String url, String user, String password) {
    return new VocabularyClient(url, user, password);
  }

  /** Returns all institutions in GrSciColl. */
  public List<Vocabulary> getVocabularies() {
    List<Vocabulary> result = new ArrayList<>();

    boolean endRecords = false;
    int offset = 0;
    while (!endRecords) {
      PagingResponse<Vocabulary> response = syncCall(api.listVocabularies(1000, offset));
      endRecords = response.isEndOfRecords();
      offset += response.getLimit();
      result.addAll(response.getResults());
    }

    return result;
  }

  public Vocabulary createVocabulary(Vocabulary vocabulary) {
    return syncCall(api.createVocabulary(vocabulary));
  }

  public Concept createConcept(String vocabularyName, Concept concept) {
    return syncCall(api.createConcept(vocabularyName, concept));
  }

  private interface API {
    @GET("vocabularies")
    Call<PagingResponse<Vocabulary>> listVocabularies(
        @Query("limit") int limit, @Query("offset") int offset);

    @POST("vocabularies")
    Call<Vocabulary> createVocabulary(@Body Vocabulary vocabulary);

    @POST("vocabularies/{vocabularyName}/concepts")
    Call<Concept> createConcept(
        @Path("vocabularyName") String vocabularyName, @Body Concept concept);
  }
}
