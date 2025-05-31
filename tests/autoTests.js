exports.autoTests = function () {
    describe('Marketplace Interface (window.plugins.marketplace)', function () {
        it('should exist', function () {
            expect(window.plugins.marketplace).toBeDefined();
        });

        it('should be available', function () {
            expect(window.plugins.marketplace.available).toBe(true);
        });

        it('should have a name', function () {
            expect(window.plugins.marketplace.name).toBeDefined();
            expect(typeof window.plugins.marketplace.name).toBe('string');
        });
    });
};
