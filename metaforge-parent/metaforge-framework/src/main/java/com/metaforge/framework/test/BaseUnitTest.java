package com.metaforge.framework.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 单元测试基类。
 * 不加载 Spring 上下文，仅提供 Mockito 支持。
 * 业务 BC 继承此类编写纯单元测试，单测试方法执行耗时应小于 100ms。
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
}
