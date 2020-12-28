package lk.sabri.inventory.util;

import org.json.JSONArray;
import org.json.JSONObject;

import lk.sabri.inventory.data.LoginObject;
import lk.sabri.inventory.data.SyncObject;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface APIInterface {

//    @FormUrlEncoded
//    @POST("/api/sync.php")
//    Call<SyncObject> syncData(@Field("lastSync") String last_sync, @Field("customer") JSONArray customers, @Field("invoices") JSONArray invoices);

    @FormUrlEncoded
    @POST("/api/sync.php")
    Call<SyncObject> syncData(@Field("data") JSONObject uploadData);

    @GET("/api/user_data.php")
    Call<LoginObject> getUsers();
}
