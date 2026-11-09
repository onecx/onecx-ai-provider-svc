# onecx-ai-provider-svc
OneCX AI Provider Backend Service

## OpenAI model configuration

OpenAI model create/update requests accept only JSON objects with optional `api` (`chat-completions` or `responses`) and string `reasoningEffort`, up to 255 characters. Blank configuration is unset; absent `api` defaults to Chat Completions. Consumers trim effort and treat blank effort as unset.
Duplicate decoded JSON keys (including escaped spellings), unknown keys, incorrect types, malformed JSON, and trailing JSON tokens are rejected with `INVALID_MODEL_CONFIG`. Validation is scoped to OpenAI; non-OpenAI configuration remains opaque.

Model updates use replacement semantics for provider, communication mode, name, and identifier: clients must resend unchanged fields. `modelConfig` alone is preserved when omitted or null, allowing legacy settings to remain until explicitly replaced.
