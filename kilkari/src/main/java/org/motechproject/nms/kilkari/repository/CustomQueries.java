package org.motechproject.nms.kilkari.repository;

import javax.jdo.Query;

import org.motechproject.mds.query.QueryExecution;
import org.motechproject.mds.query.SqlQueryExecution;
import org.motechproject.mds.util.InstanceSecurityRestriction;
import org.motechproject.nms.kilkari.domain.ActiveUser;
import org.motechproject.nms.kilkari.domain.Status;
import org.motechproject.nms.kilkari.domain.SubscriptionPack;

import java.util.List;

public class CustomQueries {

    /**
     * ActiveUserCountIncrementQuery class prepares a custom MDS query. The query
     * should increment activeUserCount.
     *
     */
    public static class ActiveUserCountIncrementQuery implements
    QueryExecution {

        private final String incrementQuery = "UPDATE org.motechproject.nms.kilkari.domain.ActiveUser " +
                "SET activeUserCount = activeUserCount + 1 WHERE index = 1";

        /**
         * This method executes the query passed.
         * @param query to be executed
         * @return List of distinct subscription packs
         */
        @Override
        public Object execute(Query query, InstanceSecurityRestriction restriction) {
            /*
             * Todo : to be implemented in next review request
             */
            return null;
        }

        /**
         * This method returns the increment query string
         * @return
         */
        public String getSqlQuery() {
            return incrementQuery;
        }
    }

    /**
     * ActiveUserCountIncrementQuery class prepares a custom MDS query. The query
     * should decrement activeUserCount.
     *
     */
    public static class ActiveUserCountDecrementQuery implements
    QueryExecution {
        
        private final String decrementQuery = "UPDATE org.motechproject.nms.kilkari.domain.ActiveUser " +
                "SET activeUserCount = activeUserCount - 1 WHERE index = 1";

        /**
         * This method executes the query passed.
         * @param query to be executed
         * @return List of distinct subscription packs
         */
        @Override
        public Object execute(Query query, InstanceSecurityRestriction restriction) {
            /*
             * Todo : to be implemented in next review request
             */
            return null;
        }

        /**
         * This method returns the increment query string
         * @return  the query string
         */
        public String getSqlQuery() {
            return decrementQuery;
        }
        
    }

    /**
     * Query to find list of Active and Pending subscription packs for given msisdn.
     */
    public static class ActiveSubscriptionQuery implements QueryExecution<List<SubscriptionPack>> {
        private String msisdn;
        private String resultParamName;

        public ActiveSubscriptionQuery(String msisdn, String resultParamName) {
            this.msisdn = msisdn;
            this.resultParamName = resultParamName;
        }

        /**
         * This method executes the query passed.
         * @param query to be executed
         * @param restriction
         * @return List of distinct subscription packs
         */
        @Override
        public List<SubscriptionPack> execute(Query query, InstanceSecurityRestriction restriction) {
            query.setFilter("msisdn == '" + msisdn + "'");
            query.setFilter("status == " + Status.ACTIVE + "or" + " status == " + Status.PENDING_ACTIVATION);
            query.setResult("DISTINCT " + resultParamName);
            return (List<SubscriptionPack>) query.execute();
        }
    }
}