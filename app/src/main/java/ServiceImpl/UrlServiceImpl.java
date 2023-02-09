package ServiceImpl;

import Model.Url;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class UrlServiceImpl {

    private  static final String url = "https://link.finhaatinsurance.com/";

    public static urlservice  urlservice= null;

    public  static urlservice getUrlservice(){
        if(urlservice == null){
            Retrofit retrofit = new Retrofit.Builder().baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            urlservice = retrofit.create(urlservice.class);
        }
        return urlservice;
    }

    public interface  urlservice{
//        getpartnerurl
        @GET("mfiurl/getpartnerurl")
        Call<Url> getUrl();
    }
}
