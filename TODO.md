## Advanced Configuration

### Custom AI Models

The plugin supports various OpenAI models:

```groovy
// Use GPT-4 for more detailed analysis
explainError(model: 'gpt-4')

// Use GPT-3.5-turbo for faster, cost-effective analysis
explainError(model: 'gpt-3.5-turbo')
```

### Log Filtering Patterns

Use regex patterns to focus analysis on specific errors:

```groovy
// Focus on compilation errors
explainError(logPattern: '(?i)(error|exception|failed|compilation)')

// Focus on test failures
explainError(logPattern: '(?i)(test.*failed|assertion.*error)')

// Focus on deployment issues
explainError(logPattern: '(?i)(deploy|connection|timeout|refused)')
```

### Performance Optimization

- **Limit log lines**: Use `maxLines` to control API costs
- **Filter logs**: Use `logPattern` to analyze only relevant errors
- **Choose appropriate models**: GPT-3.5-turbo for speed, GPT-4 for accuracy
