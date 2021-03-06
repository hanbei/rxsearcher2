package rxsearch.config.search

import rxsearch.config.ConfigurationException
import rxsearch.searcher.duckduckgo.DuckDuckGoSearcher

class DuckDuckGoSearcherFactoryTest extends SearcherNodeSpecification {

    void setup() {
        searcherFactory = new DuckDuckGoSearcherFactory()
    }

    def "returns new instance"() {
        when:
        DuckDuckGoSearcher instance = searcherFactory.newInstance(null, null, null, [name: "ddgo"]) as DuckDuckGoSearcher
        then:
        instance instanceof DuckDuckGoSearcher
        instance.getName() == "ddgo"
    }

    def "missing name throws"() {
        when:
        searcherFactory.newInstance(null, null, null, [:])
        then:
        def e = thrown(ConfigurationException)
        e.getMessage() == "Missing config value 'name' for 'DuckDuckGoSearcher'"
    }
}
