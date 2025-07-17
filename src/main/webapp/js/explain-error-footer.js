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
  `;
  document.head.appendChild(style);
  console.log('CSS styles injected');
}

function addExplainErrorButton() {
  const consoleOutput =
    document.querySelector('#out') ||
    document.querySelector('pre.console-output') ||
    document.querySelector('pre');

  if (!consoleOutput) {
    console.warn('Console output element not found');
    setTimeout(addExplainErrorButton, 3000);
    return;
  }

  if (document.querySelector('.explain-error-container')) {
    console.log('Button container already exists, skipping');
    return;
  }

  const container = document.createElement('div');
  container.className = 'explain-error-container';

  const explainBtn = createButton('Explain Error', 'btn btn-primary', explainConsoleError);
  const testBtn = createButton('Test Endpoint', 'btn btn-secondary', testEndpoint);
  const diagBtn = createButton('Diagnostic', 'btn btn-info', runDiagnostic);

  const result = document.createElement('div');
  result.id = 'explain-error-result';
  result.className = 'explain-error-result';
  result.style.display = 'none';

  container.append(explainBtn, testBtn, diagBtn, result);
  consoleOutput.parentNode.insertBefore(container, consoleOutput);
}

function createButton(text, className, onClick) {
  const btn = document.createElement('button');
  btn.textContent = text;
  btn.className = className;
  btn.style.marginRight = '10px';
  btn.onclick = onClick;
  return btn;
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

  result.style.display = 'block';
  result.innerHTML = `
    <div style="text-align: center; padding: 20px;">
      <div class="spinner"></div>
      <span>Analyzing error logs...</span>
    </div>
    <div style="margin-top: 10px; font-size: 12px; color: #666;">
      <strong>Debug Info:</strong><br/>
      Current URL: ${window.location.href}<br/>
      Request will be sent to: <span id="debug-url"></span><br/>
      Console text length: ${text.length} characters<br/>
      <span id="debug-status">Fetching CSRF token...</span>
    </div>
  `;

  fetchCrumbToken().then(crumb => {
    document.getElementById('debug-status').textContent = 'CSRF token obtained, sending request...';
    sendExplainRequest(text, crumb, result);
  }).catch(err => {
    document.getElementById('debug-status').textContent = 'Failed to get CSRF token, using fallback';
    sendExplainRequest(text, null, result);
  });
}

function sendExplainRequest(text, crumb, result) {
  const basePath = window.location.pathname.replace(/\/console$/, '');
  const url = basePath + '/console-explain-error/explainConsoleError';
  document.getElementById('debug-url').textContent = url;

  const xhr = new XMLHttpRequest();
  xhr.open('POST', url, true);
  xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
  if (crumb) xhr.setRequestHeader(crumb.headerName, crumb.value);

  xhr.timeout = 120000;
  xhr.onload = function () {
    if (xhr.status === 200) {
      try {
        const response = JSON.parse(xhr.responseText);
        result.innerHTML = '<h3>AI Error Explanation:</h3><div style="white-space: pre-wrap;">' + response + '</div>';
      } catch (e) {
        result.innerHTML = `<div style="color:red;"><strong>JSON Error:</strong> ${e.message}</div>`;
      }
    } else {
      result.innerHTML = `<div style="color:red;"><strong>Error:</strong> ${xhr.status}</div><div>${xhr.responseText}</div>`;
    }
  };
  xhr.onerror = function () {
    result.innerHTML = '<div style="color:red;"><strong>Network Error</strong></div>';
  };
  xhr.ontimeout = function () {
    result.innerHTML = '<div style="color:red;"><strong>Timeout</strong>: Request took too long</div>';
  };
  xhr.send('errorText=' + encodeURIComponent(text));
}

function testEndpoint() {
  const result = document.getElementById('explain-error-result');
  result.style.display = 'block';
  result.textContent = 'Testing endpoint...';

  const basePath = window.location.pathname.replace(/\/console$/, '');
  const url = basePath + '/console-explain-error/testEndpoint';

  const xhr = new XMLHttpRequest();
  xhr.open('GET', url, true);
  xhr.onload = function () {
    result.innerHTML =
      xhr.status === 200
        ? `<div style="color:green;">✅ Test Success!</div><div>${xhr.responseText}</div>`
        : `<div style="color:red;">❌ Test Failed! Status: ${xhr.status}</div><div>${xhr.responseText}</div>`;
  };
  xhr.send();
}

function runDiagnostic() {
  const result = document.getElementById('explain-error-result');
  result.style.display = 'block';
  result.textContent = 'Running diagnostic...';

  const basePath = window.location.pathname.replace(/\/console$/, '');
  const url = basePath + '/console-explain-error/diagnostic';

  const xhr = new XMLHttpRequest();
  xhr.open('GET', url, true);
  xhr.onload = function () {
    result.innerHTML =
      xhr.status === 200
        ? `<div style="color:green;">✅ Diagnostic Complete</div><pre>${xhr.responseText}</pre>`
        : `<div style="color:red;">❌ Diagnostic Failed! Status: ${xhr.status}</div><div>${xhr.responseText}</div>`;
  };
  xhr.send();
}

function fetchCrumbToken() {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('GET', '/crumbIssuer/api/json', true);
    xhr.onload = function () {
      if (xhr.status === 200) {
        try {
          const response = JSON.parse(xhr.responseText);
          resolve({ headerName: response.crumbRequestField, value: response.crumb });
        } catch (err) {
          reject(err);
        }
      } else {
        reject(new Error('Failed to fetch CSRF token'));
      }
    };
    xhr.onerror = () => reject(new Error('XHR Error during crumb fetch'));
    xhr.send();
  });
}
