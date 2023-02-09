package serviceApi;

import Model.Url;
import retrofit2.Call;
import retrofit2.http.GET;

public interface UrlService {

      @GET("mfiurl/geturl")
      Call<Url> getUrl();
 }
