package org.make.swift

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar

trait BaseTest
    extends FeatureSpec
    with GivenWhenThen
    with MockitoSugar
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures
