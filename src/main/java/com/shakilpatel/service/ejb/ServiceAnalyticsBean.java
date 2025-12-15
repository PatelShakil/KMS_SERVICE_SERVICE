package com.shakilpatel.service.ejb;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.logging.Logger;

@Stateless
public class ServiceAnalyticsBean {

    private static final Logger LOGGER = Logger.getLogger(ServiceAnalyticsBean.class.getName());

    @PersistenceContext(unitName = "ServicePU")
    private EntityManager em;

    // ========================================
    // INCREMENT FEEDBACK FORMS
    // ========================================
    public void incrementFeedbackForms(Long serviceId) {
        String sql = "UPDATE service_service.services " +
                "SET total_feedback_forms = total_feedback_forms + 1, " +
                "    updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceId);
        int updated = query.executeUpdate();
        em.flush();

        if (updated > 0) {
            LOGGER.info("✅ Incremented feedback forms for service: " + serviceId);
        } else {
            LOGGER.warning("⚠️ Service not found: " + serviceId);
        }
    }

    // ========================================
    // DECREMENT FEEDBACK FORMS
    // ========================================
    public void decrementFeedbackForms(Long serviceId) {
        String sql = "UPDATE service_service.services " +
                "SET total_feedback_forms = GREATEST(total_feedback_forms - 1, 0), " +
                "    updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceId);
        query.executeUpdate();
        em.flush();

        LOGGER.info("✅ Decremented feedback forms for service: " + serviceId);
    }

    // ========================================
    // INCREMENT QR CODES
    // ========================================
    public void incrementQrCodes(Long serviceId) {
        String sql = "UPDATE service_service.services " +
                "SET total_qr_codes = total_qr_codes + 1, " +
                "    updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceId);
        int updated = query.executeUpdate();
        em.flush();

        if (updated > 0) {
            LOGGER.info("✅ Incremented QR codes for service: " + serviceId);
        }
    }

    // ========================================
    // DECREMENT QR CODES
    // ========================================
    public void decrementQrCodes(Long serviceId) {
        String sql = "UPDATE service_service.services " +
                "SET total_qr_codes = GREATEST(total_qr_codes - 1, 0), " +
                "    updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceId);
        query.executeUpdate();
        em.flush();

        LOGGER.info("✅ Decremented QR codes for service: " + serviceId);
    }

    // ========================================
    // INCREMENT FEEDBACK RECEIVED
    // ========================================
    public void incrementFeedbackReceived(Long serviceId) {
        String sql = "UPDATE service_service.services " +
                "SET total_feedback_received = total_feedback_received + 1, " +
                "    updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceId);
        int updated = query.executeUpdate();
        em.flush();

        if (updated > 0) {
            LOGGER.info("✅ Incremented feedback received for service: " + serviceId);
        }
    }

    // ========================================
    // RESET ANALYTICS (OPTIONAL - FOR TESTING)
    // ========================================
    public void resetAnalytics(Long serviceId) {
        String sql = "UPDATE service_service.services " +
                "SET total_feedback_forms = 0, " +
                "    total_qr_codes = 0, " +
                "    total_feedback_received = 0, " +
                "    updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceId);
        query.executeUpdate();
        em.flush();

        LOGGER.info("✅ Reset analytics for service: " + serviceId);
    }

    // ========================================
    // GET ANALYTICS (OPTIONAL - FOR DEBUGGING)
    // ========================================
    public ServiceAnalytics getAnalytics(Long serviceId) {
        String sql = "SELECT total_feedback_forms, total_qr_codes, total_feedback_received " +
                "FROM service_service.services WHERE id = ?";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceId);

        Object[] result = (Object[]) query.getSingleResult();

        return new ServiceAnalytics(
                ((Number) result[0]).intValue(),
                ((Number) result[1]).intValue(),
                ((Number) result[2]).intValue()
        );
    }

    // Inner class for analytics result
    public static class ServiceAnalytics {
        private final int totalFeedbackForms;
        private final int totalQrCodes;
        private final int totalFeedbackReceived;

        public ServiceAnalytics(int totalFeedbackForms, int totalQrCodes, int totalFeedbackReceived) {
            this.totalFeedbackForms = totalFeedbackForms;
            this.totalQrCodes = totalQrCodes;
            this.totalFeedbackReceived = totalFeedbackReceived;
        }

        public int getTotalFeedbackForms() { return totalFeedbackForms; }
        public int getTotalQrCodes() { return totalQrCodes; }
        public int getTotalFeedbackReceived() { return totalFeedbackReceived; }

        @Override
        public String toString() {
            return String.format("Forms: %d, QR Codes: %d, Feedback: %d",
                    totalFeedbackForms, totalQrCodes, totalFeedbackReceived);
        }
    }
}
