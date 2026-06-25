onecx-ai-provider-svc  | 2026-06-25 08:56:06,258 WARN  [dev.langchain4j.agentic.agent.AgentInvocationHandler] (executor-thread-2) Improper invocation of a standalone agent outside of an agentic system, consider using AiServices instead.

onecx-ai-provider-svc  | 2026-06-25 08:54:57,688 WARNING [io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender] (vert.x-eventloop-thread-4) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: Connection refused: localhost/127.0.0.1:4317
onecx-ai-provider-svc  | 2026-06-25 08:56:05,863 INFO  [org.tkit.onecx.ai.provider.common.services.dispatch.ChatDispatchService] (executor-thread-2) Received chat request: class ChatRequestDTOV1 {
onecx-ai-provider-svc  |     requestContext: class RequestContextDTOV1 {
onecx-ai-provider-svc  |         filter: class AgentFilterDTOV1 {
onecx-ai-provider-svc  |             key: APP_ID
onecx-ai-provider-svc  |             value: ABC
onecx-ai-provider-svc  |         }
onecx-ai-provider-svc  |         aiContext: {}
onecx-ai-provider-svc  |     }
onecx-ai-provider-svc  |     chatMessage: class ChatMessageDTOV1 {
onecx-ai-provider-svc  |         conversationId: 123
onecx-ai-provider-svc  |         message: Hello santa-clause, how big is a tiger
onecx-ai-provider-svc  |         type: USER
onecx-ai-provider-svc  |         creationDate: null
onecx-ai-provider-svc  |     }
onecx-ai-provider-svc  |     conversation: null
onecx-ai-provider-svc  | }
onecx-ai-provider-svc  | 2026-06-25 08:56:05,871 INFO  [org.tkit.quarkus.jpa.daos.AbstractDAO] (executor-thread-2) Initialize the entity service org.tkit.onecx.ai.provider.domain.daos.AgentDAO_Subclass for entity class org.tkit.onecx.ai.provider.domain.models.Agent/Agent/id
onecx-ai-provider-svc  | 2026-06-25 08:56:06,071 INFO  [org.tkit.onecx.ai.provider.common.services.execution.ExecutionService] (executor-thread-2) Creating execution exec-5a96c6dca3c14dc9 for agent 1234567 in tenant default
onecx-ai-provider-svc  | 2026-06-25 08:56:06,074 INFO  [org.tkit.quarkus.jpa.daos.AbstractDAO] (executor-thread-2) Initialize the entity service org.tkit.onecx.ai.provider.domain.daos.ExecutionDAO_Subclass for entity class org.tkit.onecx.ai.provider.domain.models.Execution/Execution/id
onecx-ai-provider-svc  | 2026-06-25 08:56:06,236 INFO  [org.tkit.onecx.ai.provider.common.services.execution.ExecutionService] (executor-thread-2) Started execution exec-5a96c6dca3c14dc9
onecx-ai-provider-svc  | 2026-06-25 08:56:06,258 WARN  [dev.langchain4j.agentic.agent.AgentInvocationHandler] (executor-thread-2) Improper invocation of a standalone agent outside of an agentic system, consider using AiServices instead.
onecx-ai-provider-svc  | 2026-06-25 08:57:03,366 INFO  [dev.langchain4j.http.client.log.LoggingHttpClient] (executor-thread-2) HTTP response:
onecx-ai-provider-svc  | - status code: 200
onecx-ai-provider-svc  | - headers: [Connection: keep-alive], [Content-Length: 2041], [Content-Type: application/json; charset=utf-8], [Date: Thu, 25 Jun 2026 08:57:03 GMT], [Via: 1.1 Caddy]
onecx-ai-provider-svc  | - body: {"model":"ministral-3:8b","created_at":"2026-06-25T08:57:03.552967258Z","message":{"role":"assistant","content":"Ah, got it! Here’s how I’ll proceed with your request—keeping it concise and within the given constraints:\n\n---\n\n**Example continuation** (since no prior content was provided, I’ll illustrate how I’d adapt to a hypothetical scenario where you’d ask about Santa Claus):\n\n---\n*\"Santa Claus is known for his jolly demeanor, magical sleigh, and his mission to deliver gifts to children worldwide on Christmas Eve. He’s often depicted wearing a red suit with white fur trim, a wide-brimmed hat, and carrying a sack full of toys. His workshop, located at the North Pole, is said to be bustling with elves who help him prepare for the holiday season. Santa’s reindeer, led by Rudolph with his glowing red nose, pull his sleigh across the night sky—though some say he can also travel via time or magic!*\n\n*Fun fact: The modern image of Santa Claus is heavily influenced by the 1823 poem ‘A Visit from St. Nicholas’ (also known as ‘The Night Before Christmas’) by Clement Clarke Moore, though some credit it to his friend, Washington Irving. The Coca-Cola ads of the 1930s further cemented his iconic red suit.*\n\n*hohoho*\nyeah\"*\n\n---\n**Key notes for your reference:**\n1. **Santa-clause-skill** is triggered when Santa Claus is mentioned, ending with *\"hohoho\"* (as per your instruction).\n2. **APP_ID=ABC** is noted but not used in responses unless explicitly required (e.g., for API calls or filters).\n3. **Scaffold skills** are only applied when relevant (here, the `santa-clause-skill` was the only fit).\n\n---\n**Ready for your actual content!** Drop your question or topic, and I’ll tailor the response accordingly—always keeping it festive, accurate, and within the given rules.\n\nyeah"},"done":true,"done_reason":"stop","total_duration":56363354390,"load_duration":179027963,"prompt_eval_count":117,"prompt_eval_duration":136215837,"eval_count":397,"eval_duration":55411997749}
onecx-ai-provider-svc  |
onecx-ai-provider-svc  | 2026-06-25 08:57:03,404 INFO  [org.tkit.onecx.ai.provider.common.services.mcp.McpService] (executor-thread-2) Discovering tools from MCP server: https://onecx-docs-ai-dev.dev.one-cx.org/mcp
onecx-ai-provider-svc  | 2026-06-25 08:57:04,621 INFO  [org.tkit.onecx.ai.provider.common.services.mcp.McpService] (executor-thread-2) Discovered 1 tool(s) from https://onecx-docs-ai-dev.dev.one-cx.org/mcp
onecx-ai-provider-svc  | 2026-06-25 08:57:04,621 INFO  [org.tkit.onecx.ai.provider.common.services.mcp.McpService] (executor-thread-2) Created tool registry with 1 MCP tools from agent
onecx-ai-provider-svc  | 2026-06-25 08:57:04,627 INFO  [org.tkit.quarkus.jpa.daos.AbstractDAO] (executor-thread-2) Initialize the entity service org.tkit.onecx.ai.provider.domain.daos.ExternalAgentDAO_Subclass for entity class org.tkit.onecx.ai.provider.domain.models.ExternalAgent/ExternalAgent/id
onecx-ai-provider-svc  | 2026-06-25 08:57:08,333 WARNING [io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender] (vert.x-eventloop-thread-8) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: Connection refused: localhost/127.0.0.1:4317
onecx-ai-provider-svc  | 2026-06-25 08:57:25,301 INFO  [dev.langchain4j.http.client.log.LoggingHttpClient] (executor-thread-2) HTTP response:
onecx-ai-provider-svc  | - status code: 200
onecx-ai-provider-svc  | - headers: [Connection: keep-alive], [Content-Length: 449], [Content-Type: application/json; charset=utf-8], [Date: Thu, 25 Jun 2026 08:57:25 GMT], [Via: 1.1 Caddy]
onecx-ai-provider-svc  | - body: {"model":"ministral-3:8b","created_at":"2026-06-25T08:57:25.471513633Z","message":{"role":"assistant","content":"```json\n{\n  \"agentName\": \"onecx-agent\",\n  \"arguments\": {\n    \"userRequest\": \"Hello santa-clause, how big is a tiger\"\n  }\n}\n```"},"done":true,"done_reason":"stop","total_duration":20024575135,"load_duration":185117928,"prompt_eval_count":390,"prompt_eval_duration":14100055390,"eval_count":41,"eval_duration":5674368528}
onecx-ai-provider-svc  |
onecx-ai-provider-svc  | 2026-06-25 08:57:25,311 INFO  [dev.langchain4j.agentic.supervisor.SupervisorPlanner] (executor-thread-2) Agent Invocation: AgentInvocation{agentName='onecx-agent', arguments={userRequest=Hello santa-clause, how big is a tiger}}
onecx-ai-provider-svc  | 2026-06-25 08:57:25,323 INFO  [org.tkit.onecx.ai.provider.common.services.execution.ExecutionService] (executor-thread-2) Execution exec-5a96c6dca3c14dc9 transitioned to WAITING_AGENT
onecx-ai-provider-svc  | 2026-06-25 08:57:25,344 INFO  [org.tkit.onecx.ai.provider.common.services.execution.ExecutionService] (executor-thread-2) Creating execution exec-c8efff76696645a8 for agent onecx-agent in tenant default
onecx-ai-provider-svc  | 2026-06-25 08:57:25,361 INFO  [org.tkit.onecx.ai.provider.common.services.execution.ExecutionService] (executor-thread-2) Started execution exec-c8efff76696645a8
onecx-ai-provider-svc  | 2026-06-25 08:57:29,240 WARNING [io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender] (vert.x-eventloop-thread-8) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: Connection refused: localhost/127.0.0.1:4317
onecx-ai-provider-svc  | 2026-06-25 08:58:09,851 WARNING [io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender] (vert.x-eventloop-thread-8) Failed to export TraceRequestMarshaler. The request could not be executed. Full error message: Connection refused: localhost/127.0.0.1:4317
onecx-ai-provider-svc  | 2026-06-25 08:58:25,626 WARN  [dev.langchain4j.internal.RetryUtils] (executor-thread-2) A retriable exception occurred. Remaining retries: 2 of 2: dev.langchain4j.exception.InternalServerException:
onecx-ai-provider-svc  |        at dev.langchain4j.internal.ExceptionMapper$DefaultExceptionMapper.mapHttpStatusCode(ExceptionMapper.java:56)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.ExceptionMapper$DefaultExceptionMapper.mapException(ExceptionMapper.java:44)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.ExceptionMapper.withExceptionMapper(ExceptionMapper.java:31)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.RetryUtils.lambda$withRetryMappingExceptions$1(RetryUtils.java:326)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.RetryUtils$RetryPolicy.withRetry(RetryUtils.java:204)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.RetryUtils.withRetry(RetryUtils.java:263)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.RetryUtils.withRetryMappingExceptions(RetryUtils.java:326)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.RetryUtils.withRetryMappingExceptions(RetryUtils.java:309)
onecx-ai-provider-svc  |        at dev.langchain4j.model.ollama.OllamaChatModel.doChat(OllamaChatModel.java:41)
onecx-ai-provider-svc  |        at dev.langchain4j.model.chat.ChatModel.chat(ChatModel.java:61)
onecx-ai-provider-svc  |        at dev.langchain4j.model.chat.ChatModel.chat(ChatModel.java:36)
onecx-ai-provider-svc  |        at dev.langchain4j.guardrail.SynchronousChatExecutor.execute(SynchronousChatExecutor.java:32)
onecx-ai-provider-svc  |        at dev.langchain4j.guardrail.AbstractChatExecutor.executeInternal(AbstractChatExecutor.java:66)
onecx-ai-provider-svc  |        at dev.langchain4j.guardrail.AbstractChatExecutor.execute(AbstractChatExecutor.java:52)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.AiServiceMethodImplementationSupport.doImplement0(AiServiceMethodImplementationSupport.java:403)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.AiServiceMethodImplementationSupport.doImplement(AiServiceMethodImplementationSupport.java:209)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.AiServiceMethodImplementationSupport.implement(AiServiceMethodImplementationSupport.java:184)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$1.apply(MethodImplementationSupportProducer.java:31)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$1.apply(MethodImplementationSupportProducer.java:28)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.SpanWrapper.wrap(SpanWrapper.java:32)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:40)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:37)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MetricsTimedWrapper$2.get(MetricsTimedWrapper.java:42)
onecx-ai-provider-svc  |        at io.micrometer.core.instrument.composite.CompositeTimer.record(CompositeTimer.java:69)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MetricsTimedWrapper.wrap(MetricsTimedWrapper.java:39)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:40)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:37)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MetricsCountedWrapper.wrap(MetricsCountedWrapper.java:22)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:40)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1$2.apply(MethodImplementationSupportProducer.java:37)
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.runtime.aiservice.MethodImplementationSupportProducer$1.implement(MethodImplementationSupportProducer.java:46)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.TextAgent$$QuarkusImpl.invoke(Unknown Source)
onecx-ai-provider-svc  |        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
onecx-ai-provider-svc  |        at java.base/java.lang.reflect.Method.invoke(Method.java:565)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.agent.AgentInvocationHandler.invoke(AgentInvocationHandler.java:165)
onecx-ai-provider-svc  |        at jdk.proxy2/jdk.proxy2.$Proxy106.invoke(Unknown Source)
onecx-ai-provider-svc  |        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
onecx-ai-provider-svc  |        at java.base/java.lang.reflect.Method.invoke(Method.java:565)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.AgentInvoker.internalInvoke(AgentInvoker.java:48)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.AgentInvoker.invoke(AgentInvoker.java:34)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.AbstractAgentInvoker.invoke(AbstractAgentInvoker.java:88)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.AgentExecutor.agentResponse(AgentExecutor.java:94)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.AgentExecutor.internalExecute(AgentExecutor.java:67)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.AgentExecutor.execute(AgentExecutor.java:37)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.AgentExecutor.execute(AgentExecutor.java:25)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.PlannerBasedInvocationHandler$PlannerLoop.loop(PlannerBasedInvocationHandler.java:374)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.PlannerBasedInvocationHandler.executeAgentMethod(PlannerBasedInvocationHandler.java:203)
onecx-ai-provider-svc  |        at dev.langchain4j.agentic.internal.PlannerBasedInvocationHandler.invoke(PlannerBasedInvocationHandler.java:180)
onecx-ai-provider-svc  |        at jdk.proxy2/jdk.proxy2.$Proxy116.invoke(Unknown Source)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService.executeSupervisorRoutedGroup(AgenticRuntimeService.java:119)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService.executeGroup(AgenticRuntimeService.java:99)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService.lambda$executeGroups$2(AgenticRuntimeService.java:81)
onecx-ai-provider-svc  |        at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:214)
onecx-ai-provider-svc  |        at java.base/java.util.AbstractList$RandomAccessSpliterator.forEachRemaining(AbstractList.java:722)
onecx-ai-provider-svc  |        at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:570)
onecx-ai-provider-svc  |        at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:560)
onecx-ai-provider-svc  |        at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
onecx-ai-provider-svc  |        at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:265)
onecx-ai-provider-svc  |        at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:723)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService.executeGroups(AgenticRuntimeService.java:84)
onecx-ai-provider-svc  |        at org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService.invokeRoot(AgenticRuntimeService.java:57)
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
onecx-ai-provider-svc  | Caused by: dev.langchain4j.exception.HttpException:
onecx-ai-provider-svc  |        at io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClient.execute(JaxRsHttpClient.java:126)
onecx-ai-provider-svc  |        at dev.langchain4j.http.client.log.LoggingHttpClient.execute(LoggingHttpClient.java:49)
onecx-ai-provider-svc  |        at dev.langchain4j.model.ollama.OllamaClient.chat(OllamaClient.java:115)
onecx-ai-provider-svc  |        at dev.langchain4j.model.ollama.OllamaChatModel.lambda$doChat$0(OllamaChatModel.java:41)
onecx-ai-provider-svc  |        at dev.langchain4j.internal.ExceptionMapper.withExceptionMapper(ExceptionMapper.java:29)
onecx-ai-provider-svc  |        ... 95 more
onecx-ai-provider-svc  |