exports.autoTests = function () {
    describe('Template Interface (window.plugins.Template)', function () {
        it('should exist', function () {
            expect(window.plugins.Template).toBeDefined();
        });

        it('should be available', function () {
            expect(window.plugins.Template.available).toBe(true);
        });
    });
};
