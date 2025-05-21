exports.innerHTML = `
    <div id="initialized">press Refresh to update...</div>
    <div id="available">press Refresh to update...</div>
`;

const updateApiStatus = () => {
    const Template = window.plugins.Template;
    document.getElementById('available').innerText = Template.available ? 'available' : 'unavailable';
    document.getElementById('initialized').innerText = Template.initialized ? 'initialized' : 'uninitialized';
};

const testApi = function (createActionButton) {
    createActionButton(
        'Get Version',
        async function () {
            try {
                const Template = window.plugins.Template;
                const version = await Template.getVersion();
                updateApiStatus();
                console.log(`Plugin Version ${version}`);
            } catch (err) {
                console.error(`Error: ${err.message}`);
            }
        },
        'api'
    );
};

exports.setup = testApi;
