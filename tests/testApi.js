exports.innerHTML = `
    <div id="mp-available">press Refresh to update...</div>
    <div id="mp-name">press Refresh to update...</div>
    <div id="mp-info">press Refresh to update...</div>
`;

const updateApiStatus = () => {
    const marketplace = window.plugins.marketplace;
    document.getElementById('mp-available').innerText = marketplace.available ? 'available' : 'unavailable';
    document.getElementById('mp-name').innerText = marketplace.name ?? '<null>';
    document.getElementById('mp-info').innerHTML = `<pre>${JSON.stringify(marketplace.info, undefined, 2)}</pre>`;
};

const testApi = function (createActionButton) {
    createActionButton(
        'Refresh',
        async function () {
            try {
                updateApiStatus();
            } catch (err) {
                console.error(`Error: ${err.message}`);
            }
        },
        'api'
    );
};

exports.setup = testApi;
