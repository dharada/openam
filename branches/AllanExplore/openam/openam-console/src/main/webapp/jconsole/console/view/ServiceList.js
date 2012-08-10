/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/9/12
 * Time: 17:05
 * To change this template use File | Settings | File Templates.
 */



Ext.define('OpenAM.view.ServiceList', {
    extends: 'Ext.grid.Panel',
    alias:   'widget.servicelist',
    title:   'Services',

    initComponent: function() {
        this.margins        = '0 5 5 5';
        this.bodyPadding    = 5;
        this.columns        = [
            { text: 'Service',  dataIndex: 'name', flex: 1}
        ];
        this.callParent(arguments);
    }
});
