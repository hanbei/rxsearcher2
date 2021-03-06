package rxsearch.config.filter

import rxsearch.filter.impl.NoImageFilter


class NoImageFilterFactoryTest extends FilterNodeSpecification {

    void setup() {
        filterFactory = new NoImageFilterFactory()
    }

    def "returns new price filter"() {
        when:
        def instance = filterFactory.newInstance(null, null, null, null)
        then:
        instance instanceof NoImageFilter
    }

}
