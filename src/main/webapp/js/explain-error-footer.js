document.addEventListener('DOMContentLoaded', function () {
  console.log('DOMContentLoaded event fired');
  console.log('Current URL:', window.location.href);
  console.log('Current pathname:', window.location.pathname);

  injectStyles();

  if (
    window.location.pathname.includes('/console') &&
    !window.location.pathname.includes('/error-explanation')
  ) {
    console.log('Console page detected, adding button');
    addExplainErrorButton();
  } else {
    console.log('Not a console page or already on error explanation page, skipping');
  }
});

function injectStyles() {
  const style = document.createElement('style');
  style.type = 'text/css';
  style.innerHTML = `
    .explain-error-container {
      margin: 10px 0;
      padding: 10px;
      background: #f0f0f0;
      border: 1px solid #ccc;
      border-radius: 4px;
    }
    .explain-error-btn {
      margin-right: 5px !important;
    }
    .jenkins-button.explain-error-btn {
      display: inline-block;
      margin-right: 5px;
    }
    .explain-error-result {
      margin-top: 10px;
      padding: 10px;
      background: white;
      border: 1px solid #ddd;
      border-radius: 4px;
      max-height: 400px;
      overflow-y: auto;
    }
    .explain-error-result h3 {
      margin-top: 0;
      color: #333;
    }
    .spinner {
      border: 4px solid #f3f3f3;
      border-top: 4px solid #007acc;
      border-radius: 50%;
      width: 20px;
      height: 20px;
      animation: spin 1s linear infinite;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    /* Console output styling to match sidebar */
    #out, pre.console-output, pre {
      background: #f8f9fa !important;
      border: 1px solid #dee2e6 !important;
      border-radius: 6px !important;
      padding: 15px !important;
      margin: 10px 0 !important;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
      font-size: 13px !important;
      line-height: 1.5 !important;
      color: #495057 !important;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1) !important;
    }
    /* AI explanation header styling */
    #ai-explanation-header h3 {
      background: #e9ecef !important;
      padding: 10px 15px !important;
      margin: 10px 0 0 0 !important;
      border: 1px solid #dee2e6 !important;
      border-radius: 6px 6px 0 0 !important;
      color: #495057 !important;
      font-size: 16px !important;
      font-weight: 600 !important;
    }
  `;
  document.head.appendChild(style);
  console.log('CSS styles injected');
}

function addExplainErrorButton() {
  // First try to find the existing console button bar
  const consoleButtonBar = 
    document.querySelector('.console-actions') ||
    document.querySelector('.console-output-actions') ||
    document.querySelector('.console-controls') ||
    document.querySelector('[class*="console"][class*="button"]') ||
    document.querySelector('#console .btn-group') ||
    document.querySelector('.jenkins-button-bar');

  // Try to find buttons by their text content
  let buttonContainer = null;
  const downloadButtons = Array.from(document.querySelectorAll('a, button')).filter(el => 
    el.textContent && (
      el.textContent.includes('Download') || 
      el.textContent.includes('Copy') || 
      el.textContent.includes('View as plain text')
    )
  );

  if (downloadButtons.length > 0) {
    buttonContainer = downloadButtons[0].parentElement;
  }

  // Fallback: find console output element
  const consoleOutput =
    document.querySelector('#out') ||
    document.querySelector('pre.console-output') ||
    document.querySelector('pre');

  if (!consoleOutput && !buttonContainer) {
    console.warn('Console output element not found');
    setTimeout(addExplainErrorButton, 3000);
    return;
  }

  if (document.querySelector('.explain-error-btn')) {
    console.log('Explain Error button already exists, skipping');
    return;
  }

  const explainBtn = createButton('Explain Error', 'jenkins-button explain-error-btn', explainConsoleError);

  // If we found the button container, add our button there
  if (buttonContainer) {
    console.log('Adding button to existing button container');
    buttonContainer.insertBefore(explainBtn, buttonContainer.firstChild);
  } else if (consoleButtonBar) {
    console.log('Adding button to console button bar');
    consoleButtonBar.appendChild(explainBtn);
  } else {
    // Fallback: create a simple container above console output
    console.log('Creating new button container above console');
    const container = document.createElement('div');
    container.className = 'explain-error-container';
    container.style.marginBottom = '10px';
    container.appendChild(explainBtn);
    consoleOutput.parentNode.insertBefore(container, consoleOutput);
  }

  // Create result container (always separate from buttons)
  const result = document.createElement('div');
  result.id = 'explain-error-result';
  result.className = 'explain-error-result';
  result.style.display = 'none';
  
  // Insert result container before console output
  if (consoleOutput && consoleOutput.parentNode) {
    consoleOutput.parentNode.insertBefore(result, consoleOutput);
  } else {
    document.body.appendChild(result);
  }
}

function createButton(text, className, onClick) {
  const btn = document.createElement('button');
  btn.textContent = text;
  btn.className = className;
  btn.onclick = onClick;
  return btn;
}

function getRootURL() {
  // Extract Jenkins root URL from current page
  // This handles cases where Jenkins runs in a context path like /jenkins
  const path = window.location.pathname;
  
  // Look for common Jenkins URL patterns to determine the root
  if (path.includes('/job/')) {
    // Extract everything before /job/
    return path.substring(0, path.indexOf('/job/'));
  } else if (path.includes('/console')) {
    // Extract everything before the job-specific part
    // Pattern: /[context]/job/jobname/build/console
    const parts = path.split('/');
    let rootParts = [];
    for (let i = 0; i < parts.length; i++) {
      if (parts[i] === 'job') {
        break;
      }
      rootParts.push(parts[i]);
    }
    return rootParts.join('/') || '';
  }
  
  // Fallback: assume root context
  return '';
}

function explainConsoleError() {
  const output =
    document.querySelector('#out') ||
    document.querySelector('pre.console-output') ||
    document.querySelector('pre');
  const result = document.getElementById('explain-error-result');
  if (!output || !result) return;

  const text = output.textContent || output.innerText;
  if (!text.trim()) return alert('No console output found');

  // Show AI Error Explanation header before console output
  let explanationHeader = document.getElementById('ai-explanation-header');
  if (!explanationHeader) {
    explanationHeader = document.createElement('div');
    explanationHeader.id = 'ai-explanation-header';
    explanationHeader.innerHTML = '<h3 style="margin: 10px 0; color: #333;">AI Error Explanation:</h3>';
    output.parentNode.insertBefore(explanationHeader, result);
  }

  result.style.display = 'block';
  result.innerHTML = `
    <div style="text-align: center; padding: 20px;">
      <div class="spinner"></div>
      <span>Analyzing error logs...</span>
    </div>
    <div style="margin-top: 10px; font-size: 12px; color: #666;">
      <strong>Debug Info:</strong><br/>
      Current URL: ${window.location.href}<br/>
      Jenkins Root: ${getRootURL()}<br/>
      Request will be sent to: <span id="debug-url"></span><br/>
      Console text length: ${text.length} characters<br/>
      <span id="debug-status">Sending request...</span>
    </div>
  `;

  sendExplainRequest(text, result);
}

function sendExplainRequest(text, result) {
  const basePath = window.location.pathname.replace(/\/console$/, '');
  const url = basePath + '/console-explain-error/explainConsoleError';
  document.getElementById('debug-url').textContent = url;

  // Use Jenkins' global crumb object if available, otherwise create empty headers
  const headers = {
    "Content-Type": "application/x-www-form-urlencoded"
  };
  
  // Jenkins provides a global crumb object
  if (typeof crumb !== 'undefined' && crumb.wrap) {
    Object.assign(headers, crumb.wrap({}));
  }

  fetch(url, {
    method: "POST",
    headers: headers,
    body: 'errorText=' + encodeURIComponent(text)
  })
  .then(response => {
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return response.text();
  })
  .then(responseText => {
    try {
      // Try to parse as JSON first
      const jsonResponse = JSON.parse(responseText);
      result.innerHTML = '<div style="white-space: pre-wrap;">' + jsonResponse + '</div>';
    } catch (e) {
      // If not JSON, display as plain text
      result.innerHTML = '<div style="white-space: pre-wrap;">' + responseText + '</div>';
    }
  })
  .catch(error => {
    result.innerHTML = `<div style="color:red;"><strong>Error:</strong> ${error.message}</div>`;
  });
}
