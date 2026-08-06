package com.metaforge.common.spi;

/**
 * 健康检查扩展点接口
 * <p>用于注册自定义健康检查项，系统启动后执行健康检查并汇总结果。</p>
 *
 * @author metaforge
 */
@FunctionalInterface
public interface HealthCheckSpi {

    /**
     * 执行健康检查
     *
     * @return 健康检查结果
     */
    HealthCheckResult check();

    /**
     * 健康检查结果内部类
     */
    class HealthCheckResult {

        /** 检查项名称 */
        private final String name;

        /** 是否健康 */
        private final boolean healthy;

        /** 详细信息 */
        private final String detail;

        /**
         * 构造健康检查结果
         *
         * @param name    检查项名称
         * @param healthy 是否健康
         * @param detail  详细信息
         */
        public HealthCheckResult(String name, boolean healthy, String detail) {
            this.name = name;
            this.healthy = healthy;
            this.detail = detail;
        }

        /**
         * 获取检查项名称
         *
         * @return 检查项名称
         */
        public String getName() {
            return name;
        }

        /**
         * 是否健康
         *
         * @return 健康状态
         */
        public boolean isHealthy() {
            return healthy;
        }

        /**
         * 获取详细信息
         *
         * @return 详细信息
         */
        public String getDetail() {
            return detail;
        }
    }
}
