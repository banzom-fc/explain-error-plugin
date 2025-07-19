document.addEventListener('DOMContentLoaded', function () {
  if (
    window.location.pathname.includes('/console') &&
    !window.location.pathname.includes('/error-explanation')
  ) {
    addExplainErrorButton();
  }
});

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
    return;
  }

  const explainBtn = createButton('Explain Error', 'jenkins-button explain-error-btn', explainConsoleError);

  // If we found the button container, add our button there
  if (buttonContainer) {
    buttonContainer.insertBefore(explainBtn, buttonContainer.firstChild);
  } else if (consoleButtonBar) {
    consoleButtonBar.appendChild(explainBtn);
  } else {
    // Fallback: create a simple container above console output
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
    explanationHeader.className = 'ai-explanation-header';
    output.parentNode.insertBefore(explanationHeader, result);
  }

  result.style.display = 'block';
  result.innerHTML = `
    <div class="explain-error-loading">
      <div class="explain-error-spinner"></div>
      <span>Analyzing error logs...</span>
    </div>
  `;

  sendExplainRequest(text, result);
}

function sendExplainRequest(text, result) {
  const basePath = window.location.pathname.replace(/\/console$/, '');
  const url = basePath + '/console-explain-error/explainConsoleError';

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
      result.innerHTML = '<div class="explain-error-message">' + jsonResponse + '</div>';
    } catch (e) {
      // If not JSON, display as plain text
      result.innerHTML = '<div class="explain-error-message">' + responseText + '</div>';
    }
  })
  .catch(error => {
    result.innerHTML = `<div class="explain-error-error">Error: ${error.message}</div>`;
  });
}
