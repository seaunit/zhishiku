UPDATE billing_ai_rate AS rate
SET points_per_unit = 1
FROM billing_catalog_version AS catalog
WHERE rate.catalog_version_id = catalog.id
  AND catalog.status = 'active'
  AND rate.feature = 'ai_answer';
