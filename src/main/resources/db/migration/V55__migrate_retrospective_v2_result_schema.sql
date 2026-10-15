UPDATE retrospectives
SET result_strength = CASE
        WHEN NULLIF(TRIM(result_strength), '') IS NULL THEN NULL
        ELSE JSON_ARRAY(result_strength)
    END,
    result_improvement = CASE
        WHEN NULLIF(TRIM(result_improvement), '') IS NULL THEN NULL
        ELSE JSON_ARRAY(result_improvement)
    END,
    result_process = CASE
        WHEN NULLIF(TRIM(result_process), '') IS NULL THEN NULL
        ELSE JSON_ARRAY(result_process)
    END,
    result_learning = CASE
        WHEN NULLIF(TRIM(result_learning), '') IS NULL THEN NULL
        ELSE JSON_ARRAY(result_learning)
    END,
    result_insight = CASE
        WHEN NULLIF(TRIM(result_insight), '') IS NULL THEN NULL
        ELSE JSON_OBJECT(
            'title', '디딧의 인사이트',
            'description', result_insight
        )
    END,
    result_next_actions = CASE
        WHEN NULLIF(TRIM(result_next_actions), '') IS NULL THEN NULL
        WHEN JSON_LENGTH(result_next_actions) = 0 THEN NULL
        WHEN JSON_LENGTH(result_next_actions) = 1 THEN JSON_ARRAY(
            JSON_OBJECT(
                'title', '다음 행동',
                'description', JSON_UNQUOTE(JSON_EXTRACT(result_next_actions, '$[0]'))
            )
        )
        WHEN JSON_LENGTH(result_next_actions) = 2 THEN JSON_ARRAY(
            JSON_OBJECT(
                'title', '다음 행동',
                'description', JSON_UNQUOTE(JSON_EXTRACT(result_next_actions, '$[0]'))
            ),
            JSON_OBJECT(
                'title', '다음 행동',
                'description', JSON_UNQUOTE(JSON_EXTRACT(result_next_actions, '$[1]'))
            )
        )
        WHEN JSON_LENGTH(result_next_actions) = 3 THEN JSON_ARRAY(
            JSON_OBJECT(
                'title', '다음 행동',
                'description', JSON_UNQUOTE(JSON_EXTRACT(result_next_actions, '$[0]'))
            ),
            JSON_OBJECT(
                'title', '다음 행동',
                'description', JSON_UNQUOTE(JSON_EXTRACT(result_next_actions, '$[1]'))
            ),
            JSON_OBJECT(
                'title', '다음 행동',
                'description', JSON_UNQUOTE(JSON_EXTRACT(result_next_actions, '$[2]'))
            )
        )
        ELSE result_next_actions
    END,
    result_schema_version = 3
WHERE result_schema_version = 2
  AND CASE
      WHEN NULLIF(TRIM(result_next_actions), '') IS NULL THEN TRUE
      WHEN JSON_VALID(result_next_actions) = 0 THEN FALSE
      WHEN JSON_TYPE(result_next_actions) <> 'ARRAY' THEN FALSE
      WHEN JSON_LENGTH(result_next_actions) = 0 THEN TRUE
      WHEN JSON_LENGTH(result_next_actions) = 1 THEN
          JSON_TYPE(JSON_EXTRACT(result_next_actions, '$[0]')) = 'STRING'
      WHEN JSON_LENGTH(result_next_actions) = 2 THEN
          JSON_TYPE(JSON_EXTRACT(result_next_actions, '$[0]')) = 'STRING'
          AND JSON_TYPE(JSON_EXTRACT(result_next_actions, '$[1]')) = 'STRING'
      WHEN JSON_LENGTH(result_next_actions) = 3 THEN
          JSON_TYPE(JSON_EXTRACT(result_next_actions, '$[0]')) = 'STRING'
          AND JSON_TYPE(JSON_EXTRACT(result_next_actions, '$[1]')) = 'STRING'
          AND JSON_TYPE(JSON_EXTRACT(result_next_actions, '$[2]')) = 'STRING'
      ELSE FALSE
  END;
