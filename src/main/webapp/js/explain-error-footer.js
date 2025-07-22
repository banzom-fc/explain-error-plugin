document.addEventListener('DOMContentLoaded', function () {
  if (
    window.location.pathname.includes('/console') &&
    !window.location.pathname.includes('/error-explanation')
  ) {
    addExplainErrorButton();
  }
  // Moved from the second DOMContentLoaded listener
  const container = document.getElementById('explain-error-container');
  const consoleOutput =
    document.querySelector('#out') ||
    document.querySelector('pre.console-output') ||
    document.querySelector('pre');
  if (container && consoleOutput && consoleOutput.parentNode) {
    consoleOutput.parentNode.insertBefore(container, consoleOutput);
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
}

function createButton(text, className, onClick) {
  const btn = document.createElement('button');
  btn.textContent = text;
  btn.className = className;
  btn.onclick = onClick;
  return btn;
}

function explainConsoleError() {
  showSpinner();
  sendExplainRequest();
}

function sendExplainRequest() {
  const basePath = window.location.pathname.replace(/\/console$/, '');
  const url = basePath + '/console-explain-error/explainConsoleError';

  const headers = crumb.wrap({
    "Content-Type": "application/x-www-form-urlencoded",
  });

  // Optionally, you can add maxLines here if you want to support it from the UI
  // For now, just send an empty body
  fetch(url, {
    method: "POST",
    headers: headers,
    body: ""
  })
  .then(response => {
    if (!response.ok) {
      notificationBar.show('Explain failed', notificationBar.ERROR);
    }
    return response.text();
  })
  .then(responseText => {
    try {
      const jsonResponse = JSON.parse(responseText);
      showErrorExplanation(jsonResponse);
    } catch (e) {
      showErrorExplanation(responseText);
    }
  })
  .catch(error => {
    showErrorExplanation(`Error: ${error.message}`);
  });
}

function showErrorExplanation(message) {
  const container = document.getElementById('explain-error-container');
  const spinner = document.getElementById('explain-error-spinner');
  const content = document.getElementById('explain-error-content');

  container.classList.remove('jenkins-hidden');
  spinner.classList.add('jenkins-hidden');
  content.textContent = message;
}

function showSpinner() {
  const container = document.getElementById('explain-error-container');
  const spinner = document.getElementById('explain-error-spinner');
  container.classList.remove('jenkins-hidden');
  spinner.classList.remove('jenkins-hidden');
}

function hideErrorExplanation() {
  const container = document.getElementById('explain-error-container');
  container.classList.add('jenkins-hidden');
}
