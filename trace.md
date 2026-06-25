onecx-ai-provider-svc  | 2026-06-25 07:14:06,477 WARNING [io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender] (vert.x-eventloop-thread-6) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: Connection refused: localhost/127.0.0.1:4317
onecx-ai-provider-svc  | 2026-06-25 07:14:53,312 INFO  [dev.langchain4j.http.client.log.LoggingHttpClient] (executor-thread-1) HTTP response:
onecx-ai-provider-svc  | - status code: 200
onecx-ai-provider-svc  | - headers: [Connection: keep-alive], [Content-Length: 1350], [Content-Type: application/json; charset=utf-8], [Date: Thu, 25 Jun 2026 07:14:53 GMT], [Via: 1.1 Caddy]
onecx-ai-provider-svc  | - body: {"model":"ministral-3:8b","created_at":"2026-06-25T07:14:53.525582312Z","message":{"role":"assistant","content":"A tiger’s size can vary depending on whether it’s a male or female, but here’s a general idea:\n\n- **Siberian Tiger (the largest subspecies):**\n  - **Males:** Around **10 feet (3 meters) long** (including tail) and **3.3 feet (1 meter) tall** at the shoulder. They can weigh **500–670 lbs (225–300 kg)**.\n  - **Females:** Slightly smaller, about **8.5 feet (2.6 meters) long** and **2.8 feet (0.85 meters) tall**, weighing **265–375 lbs (120–170 kg)**.\n\n- **Bengal Tiger (commonly found in India/Asia):**\n  - **Males:** Around **9–10 feet (2.7–3 meters) long** and **3 feet (0.9 meters) tall**, weighing **300–550 lbs (136–250 kg)**.\n  - **Females:** About **7.5–8.5 feet (2.3–2.6 meters) long** and **2.7 feet (0.8 meters) tall**, weighing **165–265 lbs (75–120 kg)**.\n\nTigers are the largest cats in the world, and their size helps them hunt and dominate their habitats. Now, let me tell you something magical—imagine a tiger as big as a house, but with a heart as soft as snowflakes! *hohoho*\n\nYeah"},"done":true,"done_reason":"stop","total_duration":50311850562,"load_duration":241761430,"prompt_eval_count":213,"prompt_eval_duration":137846903,"eval_count":350,"eval_duration":49402578587}
onecx-ai-provider-svc  |
onecx-ai-provider-svc  | 2026-06-25 07:14:53,341 WARN  [org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService] (executor-thread-1) Agentic runtime failed for root agent 'yeah-agent': dev.langchain4j.agentic.agent.AgentInvocationException: Failed to invoke agent method: public abstract java.lang.Object dev.langchain4j.agentic.UntypedAgent.invoke(java.util.Map)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.agent.AgentInvocationHandler.invokeStandaloneAgent(AgentInvocationHandler.java:181)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.agent.AgentInvocationHandler.invoke(AgentInvocationHandler.java:165)
onecx-ai-provider-svc  |        at jdk.proxy2/jdk.proxy2.$Proxy106.invoke(Unknown Source)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService.invokeAgent(AgenticRuntimeService.java:229)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService.invokeRoot(AgenticRuntimeService.java:54)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService_ClientProxy.invokeRoot(Unknown Source)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.dispatch.ChatDispatchService.chat(ChatDispatchService.java:34)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.dispatch.ChatDispatchService_ClientProxy.chat(Unknown Source)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.rs.external.v1.controllers.DispatchRestV1Controller.chat(DispatchRestV1Controller.java:24)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.rs.external.v1.controllers.DispatchRestV1Controller_Subclass.chat$$superforward(Unknown Source)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.rs.external.v1.controllers.DispatchRestV1Controller_Subclass$0.apply(Unknown Source)
onecx-ai-provider-svc  |        at io.quarkus.arc.impl.AroundInvokeInvocationContext.proceed(AroundInvokeInvocationContext.java:73)
onecx-ai-provider-svc  |        at io.quarkus.arc.impl.AroundInvokeInvocationContext$NextAroundInvokeInvocationContext.proceed(AroundInvokeInvocationContext.java:97)
onecx-ai-provider-svc  |        at io.quarkus.hibernate.validator.runtime.interceptor.AbstractMethodValidationInterceptor.validateMethodInvocation(AbstractMethodValidationInterceptor.java:73)
onecx-ai-provider-svc  |        at io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveEndPointValidationInterceptor.validateMethodInvocation(ResteasyReactiveEndPointValidationInterceptor.java:21)
onecx-ai-provider-svc  |        at io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveEndPointValidationInterceptor_Bean.intercept(Unknown Source)
onecx-ai-provider-svc  |        at io.quarkus.arc.impl.InterceptorInvocation.invoke(InterceptorInvocation.java:42)
onecx-ai-provider-svc  |        at io.quarkus.arc.impl.AroundInvokeInvocationContext.proceed(AroundInvokeInvocationContext.java:70)
onecx-ai-provider-svc  |        at io.quarkus.arc.impl.AroundInvokeInvocationContext.proceed(AroundInvokeInvocationContext.java:62)
onecx-ai-provider-svc  |        at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorBase.invokeInNoTx(TransactionalInterceptorBase.java:344)
onecx-ai-provider-svc  |        at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNotSupported.doIntercept(TransactionalInterceptorNotSupported.java:39)
onecx-ai-provider-svc  |        at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorBase.intercept(TransactionalInterceptorBase.java:61)
onecx-ai-provider-svc  |        at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNotSupported.intercept(TransactionalInterceptorNotSupported.java:26)
onecx-ai-provider-svc  |        at io.quarkus.narayana.jta.runtime.interceptor.TransactionalInterceptorNotSupported_Bean.intercept(Unknown Source)
onecx-ai-provider-svc  |        at io.quarkus.arc.impl.InterceptorInvocation.invoke(InterceptorInvocation.java:42)
onecx-ai-provider-svc  |        at io.quarkus.arc.impl.AroundInvokeInvocationContext.perform(AroundInvokeInvocationContext.java:30)
onecx-ai-provider-svc  |        at io.quarkus.arc.impl.InvocationContexts.performAroundInvoke(InvocationContexts.java:27)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.rs.external.v1.controllers.DispatchRestV1Controller_Subclass.chat(Unknown Source)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.rs.external.v1.controllers.DispatchRestV1Controller_ClientProxy.chat(Unknown Source)
onecx-ai-provider-svc  |        at gen.org.tkit.onecx.ai.provider.rs.external.v1.DispatchV1Api$quarkusrestinvoker$chat_67f12aedfee45f7ae361fcdda5b9584d37e5ac29.invoke(Unknown Source)
onecx-ai-provider-svc  |        at org.jboss.resteasy.reactive.server.handlers.InvocationHandler.handle(InvocationHandler.java:29)
onecx-ai-provider-svc  |        at io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext.invokeHandler(QuarkusResteasyReactiveRequestContext.java:190)
onecx-ai-provider-svc  |        at org.jboss.resteasy.reactive.common.core.AbstractResteasyReactiveContext.run(AbstractResteasyReactiveContext.java:147)
onecx-ai-provider-svc  |        at io.quarkus.vertx.core.runtime.VertxCoreRecorder$15.runWith(VertxCoreRecorder.java:677)
onecx-ai-provider-svc  |        at org.jboss.threads.EnhancedQueueExecutor$Task.doRunWith(EnhancedQueueExecutor.java:2651)
onecx-ai-provider-svc  |        at org.jboss.threads.EnhancedQueueExecutor$Task.run(EnhancedQueueExecutor.java:2630)
onecx-ai-provider-svc  |        at org.jboss.threads.EnhancedQueueExecutor.runThreadBody(EnhancedQueueExecutor.java:1622)
onecx-ai-provider-svc  |        at org.jboss.threads.EnhancedQueueExecutor$ThreadBody.run(EnhancedQueueExecutor.java:1589)
onecx-ai-provider-svc  |        at org.jboss.threads.DelegatingRunnable.run(DelegatingRunnable.java:11)
onecx-ai-provider-svc  |        at org.jboss.threads.ThreadLocalResettingRunnable.run(ThreadLocalResettingRunnable.java:11)
onecx-ai-provider-svc  |        at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
onecx-ai-provider-svc  |        at java.base/java.lang.Thread.run(Thread.java:1474)
onecx-ai-provider-svc  | Caused by: java.lang.reflect.InvocationTargetException
onecx-ai-provider-svc  |        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:119)
onecx-ai-provider-svc  |        at java.base/java.lang.reflect.Method.invoke(Method.java:565)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.agent.AgentInvocationHandler.invokeStandaloneAgent(AgentInvocationHandler.java:179)
onecx-ai-provider-svc  |        ... 41 more
onecx-ai-provider-svc  | Caused by: dev.langchain4j.service.output.OutputParsingException: Failed to parse "A tiger’s size can vary depending on whether it’s a male or female, but here’s a general idea:
onecx-ai-provider-svc  |
onecx-ai-provider-svc  | - **Siberian Tiger (the largest subspecies):**
onecx-ai-provider-svc  |   - **Males:** Around **10 feet (3 meters) long** (including tail) and **3.3 feet (1 meter) tall** at the shoulder. They can weigh **500–670 lbs (225–300 kg)**.
onecx-ai-provider-svc  |   - **Females:** Slightly smaller, about **8.5 feet (2.6 meters) long** and **2.8 feet (0.85 meters) tall**, weighing **265–375 lbs (120–170 kg)**.
onecx-ai-provider-svc  |
onecx-ai-provider-svc  | - **Bengal Tiger (commonly found in India/Asia):**
onecx-ai-provider-svc  |   - **Males:** Around **9–10 feet (2.7–3 meters) long** and **3 feet (0.9 meters) tall**, weighing **300–550 lbs (136–250 kg)**.
onecx-ai-provider-svc  |   - **Females:** About **7.5–8.5 feet (2.3–2.6 meters) long** and **2.7 feet (0.8 meters) tall**, weighing **165–265 lbs (75–120 kg)**.
onecx-ai-provider-svc  |
onecx-ai-provider-svc  | Tigers are the largest cats in the world, and their size helps them hunt and dominate their habitats. Now, let me tell you something magical—imagine a tiger as big as a house, but with a heart as soft as snowflakes! *hohoho*
onecx-ai-provider-svc  |
onecx-ai-provider-svc  | Yeah" (base64: "QSB0aWdlcuKAmXMgc2l6ZSBjYW4gdmFyeSBkZXBlbmRpbmcgb24gd2hldGhlciBpdOKAmXMgYSBtYWxlIG9yIGZlbWFsZSwgYnV0IGhlcmXigJlzIGEgZ2VuZXJhbCBpZGVhOgoKLSAqKlNpYmVyaWFuIFRpZ2VyICh0aGUgbGFyZ2VzdCBzdWJzcGVjaWVzKToqKgogIC0gKipNYWxlczoqKiBBcm91bmQgKioxMCBmZWV0ICgzIG1ldGVycykgbG9uZyoqIChpbmNsdWRpbmcgdGFpbCkgYW5kICoqMy4zIGZlZXQgKDEgbWV0ZXIpIHRhbGwqKiBhdCB0aGUgc2hvdWxkZXIuIFRoZXkgY2FuIHdlaWdoICoqNTAw4oCTNjcwIGxicyAoMjI14oCTMzAwIGtnKSoqLgogIC0gKipGZW1hbGVzOioqIFNsaWdodGx5IHNtYWxsZXIsIGFib3V0ICoqOC41IGZlZXQgKDIuNiBtZXRlcnMpIGxvbmcqKiBhbmQgKioyLjggZmVldCAoMC44NSBtZXRlcnMpIHRhbGwqKiwgd2VpZ2hpbmcgKioyNjXigJMzNzUgbGJzICgxMjDigJMxNzAga2cpKiouCgotICoqQmVuZ2FsIFRpZ2VyIChjb21tb25seSBmb3VuZCBpbiBJbmRpYS9Bc2lhKToqKgogIC0gKipNYWxlczoqKiBBcm91bmQgKio54oCTMTAgZmVldCAoMi434oCTMyBtZXRlcnMpIGxvbmcqKiBhbmQgKiozIGZlZXQgKDAuOSBtZXRlcnMpIHRhbGwqKiwgd2VpZ2hpbmcgKiozMDDigJM1NTAgbGJzICgxMzbigJMyNTAga2cpKiouCiAgLSAqKkZlbWFsZXM6KiogQWJvdXQgKio3LjXigJM4LjUgZmVldCAoMi4z4oCTMi42IG1ldGVycykgbG9uZyoqIGFuZCAqKjIuNyBmZWV0ICgwLjggbWV0ZXJzKSB0YWxsKiosIHdlaWdoaW5nICoqMTY14oCTMjY1IGxicyAoNzXigJMxMjAga2cpKiouCgpUaWdlcnMgYXJlIHRoZSBsYXJnZXN0IGNhdHMgaW4gdGhlIHdvcmxkLCBhbmQgdGhlaXIgc2l6ZSBoZWxwcyB0aGVtIGh1bnQgYW5kIGRvbWluYXRlIHRoZWlyIGhhYml0YXRzLiBOb3csIGxldCBtZSB0ZWxsIHlvdSBzb21ldGhpbmcgbWFnaWNhbOKAlGltYWdpbmUgYSB0aWdlciBhcyBiaWcgYXMgYSBob3VzZSwgYnV0IHdpdGggYSBoZWFydCBhcyBzb2Z0IGFzIHNub3dmbGFrZXMhICpob2hvaG8qCgpZZWFo") into java.lang.Object
onecx-ai-provider-svc  |        at dev.langchain4j.service.output.ParsingUtils.outputParsingException(ParsingUtils.java:106)
onecx-ai-provider-svc  |        at dev.langchain4j.service.output.PojoOutputParser.parse(PojoOutputParser.java:64)
onecx-ai-provider-svc  |        at dev.langchain4j.service.output.ServiceOutputParser.parseText(ServiceOutputParser.java:70)
onecx-ai-provider-svc  |        at dev.langchain4j.service.output.ServiceOutputParser.parse(ServiceOutputParser.java:56)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.AiServiceMethodImplementationSupport.doImplement0(AiServiceMethodImplementationSupport.java:619)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.AiServiceMethodImplementationSupport.doImplement(AiServiceMethodImplementationSupport.java:209)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.AiServiceMethodImplementationSupport.implement(AiServiceMethodImplementationSupport.java:184)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$1.apply(MethodImplementationSupportProducer.java:31)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$1.apply(MethodImplementationSupportProducer.java:28)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MetricsCountedWrapper.wrap(MetricsCountedWrapper.java:22)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:40)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:37)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MetricsTimedWrapper$2.get(MetricsTimedWrapper.java:42)
onecx-ai-provider-svc  |        at io.micrometer.core.instrument.composite.CompositeTimer.record(CompositeTimer.java:69)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MetricsTimedWrapper.wrap(MetricsTimedWrapper.java:39)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:40)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:37)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.SpanWrapper.wrap(SpanWrapper.java:32)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:40)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:37)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1.implement(MethodImplementationSupportProducer.java:46)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.UntypedAgent$$QuarkusImpl.invoke(Unknown Source)
onecx-ai-provider-svc  |        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
onecx-ai-provider-svc  |        ... 43 more
onecx-ai-provider-svc  | Caused by: java.io.UncheckedIOException: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'A': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
onecx-ai-provider-svc  |  at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 1]
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$Codec.fromJson(QuarkusJsonCodecFactory.java:80)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.Json.fromJson(Json.java:82)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.JsonParsingUtils.lambda$extractAndParseJson$0(JsonParsingUtils.java:17)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.JsonParsingUtils.extractAndParseJson(JsonParsingUtils.java:23)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.JsonParsingUtils.extractAndParseJson(JsonParsingUtils.java:17)
onecx-ai-provider-svc  |        at dev.langchain4j.service.output.PojoOutputParser.parse(PojoOutputParser.java:61)
onecx-ai-provider-svc  |        ... 64 more
onecx-ai-provider-svc  | Caused by: com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'A': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
onecx-ai-provider-svc  |  at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 1]
onecx-ai-provider-svc  |        at com.fasterxml.jackson.core.JsonParser._constructReadException(JsonParser.java:2675)
onecx-ai-provider-svc  |        at com.fasterxml.jackson.core.json.ReaderBasedJsonParser._reportInvalidToken(ReaderBasedJsonParser.java:3025)
onecx-ai-provider-svc  |        at com.fasterxml.jackson.core.json.ReaderBasedJsonParser._handleOddValue(ReaderBasedJsonParser.java:2051)
onecx-ai-provider-svc  |        at com.fasterxml.jackson.core.json.ReaderBasedJsonParser.nextToken(ReaderBasedJsonParser.java:780)
onecx-ai-provider-svc  |        at com.fasterxml.jackson.databind.ObjectMapper._initForReading(ObjectMapper.java:5139)
onecx-ai-provider-svc  |        at com.fasterxml.jackson.databind.ObjectMapper._readMapAndClose(ObjectMapper.java:5042)
onecx-ai-provider-svc  |        at com.fasterxml.jackson.databind.ObjectMapper.readValue(ObjectMapper.java:3954)
onecx-ai-provider-svc  |        at com.fasterxml.jackson.databind.ObjectMapper.readValue(ObjectMapper.java:3922)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.QuarkusJsonCodecFactory$Codec.fromJson(QuarkusJsonCodecFactory.java:72)
onecx-ai-provider-svc  |        ... 69 more
onecx-ai-provider-svc  |
onecx-ai-provider-svc  | 2026-06-25 07:14:53,358 WARN  [org.tkit.onecx.ai.provider.common.services.execution.ExecutionService] (executor-thread-1) Execution exec-d9ccefce4c8e4dfb failed: type=AgentInvocationException, message=Failed to invoke agent method: public abstract java.lang.Object dev.langchain4j.agentic.UntypedAgent.invoke(java.util.Map)
onecx-ai-provider-svc  | 2026-06-25 07:14:53,383 INFO  [org.tkit.onecx.ai.provider.rs.external.v1.controllers.DispatchRestV1Controller] (executor-thread-1) POST /v1/dispatch/chat [400] [111.781s]
onecx-ai-provider-svc  | 2026-06-25 07:14:57,237 WARNING [io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender] (vert.x-eventloop-thread-6) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: Connection refused: localhost/127.0.0.1:4317