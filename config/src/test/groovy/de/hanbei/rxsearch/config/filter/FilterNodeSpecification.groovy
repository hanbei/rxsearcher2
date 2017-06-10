package de.hanbei.rxsearch.config.filter

import de.hanbei.rxsearch.config.FilterConfiguration
import de.hanbei.rxsearch.filter.HitFilter
import de.hanbei.rxsearch.model.Hit
import de.hanbei.rxsearch.model.Query
import io.reactivex.Observable
import spock.lang.Specification

abstract class FilterNodeSpecification extends Specification {

    protected AbstractFilterFactory filterFactory

    def "doesn't handle node attributes"() {
        when:
        def attribute = filterFactory.onHandleNodeAttributes(null, null, null)
        then:
        attribute == false

    }

    def "should be a leaf"() {
        when:
        def isLeaf = filterFactory.isLeaf()
        then:
        isLeaf == true
    }

    def "adds to filter parent"() {
        given:
        def filterConfig = new FilterConfiguration()
        def filter = new HitFilter() {
            @Override
            Observable<Hit> filter(Query query, Observable<Hit> observable) {
                return null
            }
        }
        when:
        filterFactory.setParent(null, filterConfig, filter)
        then:
        filterConfig.filters.size() == 1
        filterConfig.filters.get(0) == filter
    }


}
