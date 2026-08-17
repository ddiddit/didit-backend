-- :retrospective_id를 조회할 회고의 UUID 문자열로 바꾼 뒤 실행합니다.
SET @retrospective_id = UUID_TO_BIN(:retrospective_id);

SELECT
    BIN_TO_UUID(id) AS retrospective_id,
    flow_version,
    status,
    conversation_status,
    conversation_finished_at,
    input_tokens,
    output_tokens
FROM retrospectives
WHERE id = @retrospective_id;

SELECT
    BIN_TO_UUID(id) AS message_id,
    sender,
    message_type,
    question_type,
    content,
    supporting_content,
    relevance,
    included_in_result,
    created_at
FROM chat_messages
WHERE retrospective_id = @retrospective_id
ORDER BY created_at, id;

SELECT
    turn_number,
    BIN_TO_UUID(client_message_id) AS client_message_id,
    status,
    attempt_count,
    question_target,
    error_code,
    input_tokens,
    output_tokens,
    completed_at
FROM retrospective_conversation_turns
WHERE retrospective_id = @retrospective_id
ORDER BY turn_number;

SELECT
    item_type,
    status,
    summary,
    GROUP_CONCAT(BIN_TO_UUID(e.message_id) ORDER BY e.created_at) AS evidence_message_ids
FROM retrospective_analysis_items i
LEFT JOIN retrospective_analysis_evidences e ON e.analysis_item_id = i.id
WHERE i.retrospective_id = @retrospective_id
GROUP BY i.id, i.item_type, i.status, i.summary
ORDER BY i.item_type;
