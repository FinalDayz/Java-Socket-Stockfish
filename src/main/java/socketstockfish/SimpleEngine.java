package socketstockfish;

import com.google.gson.JsonObject;
import xyz.niflheim.stockfish.StockfishClient;
import xyz.niflheim.stockfish.engine.enums.Query;
import xyz.niflheim.stockfish.engine.enums.QueryType;
import xyz.niflheim.stockfish.engine.enums.Variant;
import xyz.niflheim.stockfish.exceptions.StockfishInitException;

import java.util.function.Consumer;

public class SimpleEngine implements Consumer<String> {

    String response = null;
    private StockfishClient client;

    public SimpleEngine() {

    }

    public void requestEngine(Query query) throws StockfishInitException {
        this.client = new StockfishClient.Builder()
                .setInstances(1)
                .setVariant(Variant.BMI2)
                .build();

        client.submit(query, this);

    }


    public JsonObject getResponse() {
        if(this.response != null) {
            String res = this.response;
            this.response = null;
            return parseEngineResponse(res);
        }

        return null;
    }



    private JsonObject parseEngineResponse(String engineResponse) {
        String[] splitted = engineResponse.split(" ");
        JsonObject response = new JsonObject();

        for(int i = 0; i < Math.floor(splitted.length/2.0)*2; i+= 2) {
            try{
                response.addProperty(splitted[i], Integer.parseInt(splitted[i+1]));
            } catch(Exception e) {
                response.addProperty(splitted[i], splitted[i+1]);
            }

        }

        return response;
    }

    @Override
    public void accept(String s) {
        this.response = s;
        this.client.exit();
    }

    public String getRawResponse() {
        if(this.response != null) {
            String res = this.response;
            this.response = null;
            return res;
        }

        return null;
    }
}
