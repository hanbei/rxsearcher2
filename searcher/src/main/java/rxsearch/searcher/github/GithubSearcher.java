package rxsearch.searcher.github;

import rxsearch.searcher.AbstractSearcher;
import okhttp3.OkHttpClient;

public class GithubSearcher extends AbstractSearcher {

    public GithubSearcher(String name, String repo, OkHttpClient asyncHttpClient) {
        super(name, new GithubRequestBuilder(repo), new GithubResponseParser(name), asyncHttpClient);
    }


}
